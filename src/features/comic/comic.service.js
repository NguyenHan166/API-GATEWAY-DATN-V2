import { randomUUID } from "crypto";
import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { withRetry } from "../../utils/retry.js";
import {
    uploadBufferToR2,
    buildPublicUrl,
    getImageUrl,
} from "../../integrations/r2/storage.service.js";
import { PERF } from "../../config/perf.js";
import { logger } from "../../config/logger.js";

const GEMINI_MODEL = process.env.GEMINI_MODEL_ID || "google/gemini-2.5-flash";
const NANO_BANANA_MODEL = "google/nano-banana";

const FETCH_TIMEOUT_MS = Number(process.env.HTTP_TIMEOUT_MS || 30000);

// Comic page constants
const PAGE_WIDTH = 1080;
const PAGE_HEIGHT = 1620;
const BUBBLE_PADDING = 14;
const BUBBLE_TAIL_HEIGHT = 16;
const BUBBLE_FONT_SIZE = 20;

function buildScriptPrompt({ userPrompt, pages, panelsPerPage }) {
    const totalPanels = pages * panelsPerPage;
    return `
Bạn là biên kịch truyện tranh chuyên nghiệp. Dựa trên câu chuyện của người dùng, tạo storyboard JSON chi tiết.

YÊU CẦU:
- Tạo ${totalPanels} panel (${pages} trang × ${panelsPerPage} panel/trang)
- Mỗi panel cần: id, description_vi (mô tả cảnh bằng tiếng Việt), description_en (mô tả ngắn gọn bằng tiếng Anh cho AI vẽ), dialogue (lời thoại tiếng Việt, ≤50 ký tự), speaker (tên nhân vật), emotion (happy|sad|angry|surprised|neutral)
- Lời thoại phải tự nhiên, ngắn gọn, phù hợp với nhân vật
- Mô tả tiếng Anh (description_en) phải rõ ràng về: nhân vật, bối cảnh, hành động, ánh sáng, góc máy

ĐỊNH DẠNG JSON:
{
  "story_id": "story-id-unique",
  "panels": [
    {
      "id": 1,
      "description_vi": "Mô tả cảnh bằng tiếng Việt (1-2 câu)",
      "description_en": "English visual description for AI image generation",
      "dialogue": "Lời thoại tiếng Việt ngắn gọn",
      "speaker": "Tên nhân vật",
      "emotion": "happy"
    }
  ]
}

CÂU CHUYỆN NGƯỜI DÙNG:
${userPrompt}

Tạo storyboard JSON hoàn chỉnh với ${totalPanels} panels:`.trim();
}

function buildNanoBananaPrompt({ panels, pages, panelsPerPage, style }) {
    const stylePrefix = style || "comic book style art";
    const qualitySuffix =
        "drawing, by Dave Stevens, by Adam Hughes, 1940's, 1950's, hand-drawn, color, high resolution, best quality";

    // Build detailed page-by-page script
    const pageScripts = [];
    for (let pageIdx = 0; pageIdx < pages; pageIdx++) {
        const startPanel = pageIdx * panelsPerPage;
        const endPanel = startPanel + panelsPerPage;
        const pagePanels = panels.slice(startPanel, endPanel);

        const panelDescriptions = pagePanels
            .map((p, idx) => {
                const panelNum = idx + 1;
                return `**Panel ${panelNum}** *Description:* ${p.description_en}`;
            })
            .join("  \n");

        pageScripts.push(`### Page ${pageIdx + 1}\n${panelDescriptions}`);
    }

    const fullScript = pageScripts.join("\n\n");

    // Build final prompt with clear multi-page instruction
    let layoutInstruction = "";
    if (pages === 1) {
        layoutInstruction = `single comic book page with ${panelsPerPage} panels arranged in a grid layout`;
    } else if (pages === 2) {
        layoutInstruction = `two comic book pages displayed side by side horizontally, each page has ${panelsPerPage} panels in grid layout`;
    } else {
        layoutInstruction = `${pages} comic book pages arranged horizontally in a row, each page contains ${panelsPerPage} panels in grid layout`;
    }

    return `${stylePrefix} of ${layoutInstruction}. ${fullScript}. ${qualitySuffix}`.trim();
}

function extractJson(text) {
    if (!text) throw new Error("Empty response from model");

    const candidates = [];
    if (typeof text === "string") candidates.push(text);
    if (Array.isArray(text)) candidates.push(text.join(""));
    if (text?.output) candidates.push(text.output);

    const rawStr = candidates
        .flat()
        .filter(Boolean)
        .map((x) => (Array.isArray(x) ? x.join("") : String(x)))
        .join("\n")
        .trim();

    if (!rawStr) {
        throw new Error("Empty response from model");
    }

    // Try direct parse
    try {
        return JSON.parse(rawStr);
    } catch (_) {}

    // Try extract JSON from markdown
    const match = rawStr.match(/\{[\s\S]*\}/);
    if (match) {
        try {
            return JSON.parse(match[0]);
        } catch (_) {}
    }

    throw new Error("Model did not return valid JSON");
}

function normalizePanels(panels, totalPanels, fallbackPrompt) {
    const safe = Array.isArray(panels) ? panels : [];
    const result = [];

    for (let i = 0; i < totalPanels; i++) {
        const panel = safe[i] || {};
        result.push({
            id: panel.id || i + 1,
            description_vi:
                panel.description_vi ||
                `Cảnh ${i + 1}: ${fallbackPrompt || "Truyện tranh"}`,
            description_en:
                panel.description_en || `Scene ${i + 1} of the comic story`,
            dialogue: (panel.dialogue || "").toString().trim(),
            speaker: (panel.speaker || "Narrator").toString().trim(),
            emotion: ["happy", "sad", "angry", "surprised", "neutral"].includes(
                panel.emotion
            )
                ? panel.emotion
                : "neutral",
        });
    }

    return result;
}

function escapeXml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&apos;");
}

function wrapText(text, maxChars) {
    if (!text) return [""];

    const words = text.split(/\s+/);
    const lines = [];
    let current = "";

    for (const word of words) {
        const testLine = current ? `${current} ${word}` : word;

        if (testLine.length > maxChars && current) {
            lines.push(current.trim());
            current = word;
        } else {
            current = testLine;
        }
    }

    if (current.trim()) {
        lines.push(current.trim());
    }

    return lines.length > 0 ? lines : [""];
}

function buildSpeechBubbleSvg({ text, maxWidth }) {
    if (!text) return null;

    const avgCharWidth = BUBBLE_FONT_SIZE * 0.5;
    const maxChars = Math.floor((maxWidth - BUBBLE_PADDING * 2) / avgCharWidth);
    const lines = wrapText(text, Math.max(15, maxChars));
    const lineHeight = BUBBLE_FONT_SIZE + 8;
    const bodyHeight = BUBBLE_PADDING * 2 + lines.length * lineHeight + 4;
    const bubbleHeight = bodyHeight + BUBBLE_TAIL_HEIGHT;
    const bubbleWidth = Math.min(
        maxWidth,
        Math.max(240, BUBBLE_PADDING * 2 + maxChars * avgCharWidth)
    );

    const filterId = `shadow-${Math.random().toString(36).substr(2, 9)}`;
    const textYStart = BUBBLE_PADDING + 4;

    const textNodes = lines
        .map(
            (line, idx) =>
                `<text x="${BUBBLE_PADDING + 2}" y="${
                    textYStart + idx * lineHeight
                }" font-size="${BUBBLE_FONT_SIZE}" font-family="'Noto Sans', 'Segoe UI', Arial, sans-serif" font-weight="600" fill="#1a1a1a" dominant-baseline="hanging" xml:space="preserve">${escapeXml(
                    line
                )}</text>`
        )
        .join("\n    ");

    return `
<svg xmlns="http://www.w3.org/2000/svg" width="${bubbleWidth}" height="${bubbleHeight}">
  <defs>
    <filter id="${filterId}" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur in="SourceAlpha" stdDeviation="3"/>
      <feOffset dx="2" dy="3" result="offsetblur"/>
      <feComponentTransfer>
        <feFuncA type="linear" slope="0.2"/>
      </feComponentTransfer>
      <feMerge>
        <feMergeNode/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>
  <path d="M10 0 h ${bubbleWidth - 20} a10 10 0 0 1 10 10 v ${
        bubbleHeight - BUBBLE_TAIL_HEIGHT - 20
    } a10 10 0 0 1 -10 10 h -${bubbleWidth - 20} a10 10 0 0 1 -10 -10 v -${
        bubbleHeight - BUBBLE_TAIL_HEIGHT - 20
    } a10 10 0 0 1 10 -10 z" fill="rgba(0,0,0,0.08)" transform="translate(3,4)" />
  <path d="M10 0 h ${bubbleWidth - 20} a10 10 0 0 1 10 10 v ${
        bubbleHeight - BUBBLE_TAIL_HEIGHT - 20
    } a10 10 0 0 1 -10 10 h -${bubbleWidth - 20} a10 10 0 0 1 -10 -10 v -${
        bubbleHeight - BUBBLE_TAIL_HEIGHT - 20
    } a10 10 0 0 1 10 -10 z" fill="white" stroke="#2c2c2c" stroke-width="2.5" stroke-linejoin="round" filter="url(#${filterId})" />
  <path d="M${bubbleWidth * 0.15} ${bubbleHeight - BUBBLE_TAIL_HEIGHT} l 20 ${
        BUBBLE_TAIL_HEIGHT - 2
    } l -10 -${
        BUBBLE_TAIL_HEIGHT + 2
    }" fill="rgba(0,0,0,0.08)" transform="translate(3,4)" />
  <path d="M${bubbleWidth * 0.15} ${bubbleHeight - BUBBLE_TAIL_HEIGHT} l 20 ${
        BUBBLE_TAIL_HEIGHT - 2
    } l -10 -${
        BUBBLE_TAIL_HEIGHT + 2
    }" fill="white" stroke="#2c2c2c" stroke-width="2.5" stroke-linejoin="round" />
  <g>
    ${textNodes}
  </g>
</svg>`.trim();
}

async function fetchBuffer(url, timeoutMs = FETCH_TIMEOUT_MS) {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), timeoutMs);
    try {
        const resp = await fetch(url, { signal: ctrl.signal });
        if (!resp.ok) {
            throw new Error(`Fetch failed ${resp.status}`);
        }
        const ab = await resp.arrayBuffer();
        return Buffer.from(ab);
    } finally {
        clearTimeout(timer);
    }
}

async function runGeminiStoryboard({
    prompt,
    pages,
    panelsPerPage,
    requestId,
}) {
    const totalPanels = pages * panelsPerPage;
    const scriptPrompt = buildScriptPrompt({
        userPrompt: prompt,
        pages,
        panelsPerPage,
    });

    const retryOpts = {
        retries: PERF.retry.retries || 2,
        baseDelayMs: PERF.retry.minTimeoutMs || 600,
        factor: PERF.retry.factor || 2,
    };

    const llmOutput = await withRetry(
        () =>
            withReplicateLimiter(() =>
                replicate.run(GEMINI_MODEL, {
                    input: {
                        prompt: scriptPrompt,
                        max_output_tokens: 8000,
                        temperature: 0.3,
                    },
                })
            ),
        retryOpts
    );

    let storyboard;
    try {
        storyboard = extractJson(llmOutput);
    } catch (err) {
        logger.warn(
            { requestId, model: GEMINI_MODEL, err: err?.message },
            "Gemini returned invalid JSON, using fallback"
        );
        storyboard = {
            story_id: randomUUID(),
            panels: [],
        };
    }

    const storyId = storyboard.story_id || randomUUID();
    const panels = normalizePanels(storyboard.panels, totalPanels, prompt);

    logger.info(
        {
            requestId,
            storyId,
            model: GEMINI_MODEL,
            panelCount: panels.length,
        },
        "Generated comic storyboard with Vietnamese dialogue"
    );

    return { storyId, panels };
}

async function runNanaBanana({
    panels,
    pages,
    panelsPerPage,
    style,
    requestId,
}) {
    const prompt = buildNanoBananaPrompt({
        panels,
        pages,
        panelsPerPage,
        style,
    });

    logger.info(
        { requestId, promptLength: prompt.length, pages, panelsPerPage },
        "Built Nano Banana prompt (multi-page layout)"
    );

    const retryOpts = {
        retries: PERF.retry.retries || 2,
        baseDelayMs: PERF.retry.minTimeoutMs || 600,
        factor: PERF.retry.factor || 2,
    };

    return await withRetry(
        () =>
            withReplicateLimiter(() =>
                replicate.run(NANO_BANANA_MODEL, {
                    input: {
                        prompt,
                        aspect_ratio: "2:3",
                    },
                })
            ),
        {
            ...retryOpts,
            beforeRetry: ({ attempt }) =>
                logger.warn(
                    { attempt, model: NANO_BANANA_MODEL, requestId },
                    "Retrying Nano Banana generation"
                ),
        }
    );
}

async function resolveOutputBuffer(output) {
    const first =
        Array.isArray(output) && output.length > 0
            ? output[0]
            : output?.output || output;

    if (!first) throw new Error("Model returned empty output");

    if (typeof first === "string") {
        return await fetchBuffer(first);
    }
    if (typeof first?.blob === "function") {
        const blob = await first.blob();
        const ab = await blob.arrayBuffer();
        return Buffer.from(ab);
    }
    if (first?.url) {
        return await fetchBuffer(first.url);
    }
    if (Buffer.isBuffer(first)) return first;
    return Buffer.from(first);
}

async function overlayVietnameseBubbles({
    baseImageBuffer,
    panels,
    pages,
    panelsPerPage,
}) {
    const baseImage = sharp(baseImageBuffer);
    const metadata = await baseImage.metadata();
    const imageWidth = metadata.width || PAGE_WIDTH;
    const imageHeight = metadata.height || PAGE_HEIGHT;

    const composites = [];

    // Calculate page layout (side by side if multiple pages)
    const pageWidth = Math.floor(imageWidth / pages);
    const pageHeight = imageHeight;

    // Calculate panel grid within each page
    const panelCols =
        panelsPerPage <= 4 ? 2 : Math.ceil(Math.sqrt(panelsPerPage));
    const panelRows = Math.ceil(panelsPerPage / panelCols);
    const panelWidth = Math.floor(pageWidth / panelCols);
    const panelHeight = Math.floor(pageHeight / panelRows);

    for (let i = 0; i < panels.length; i++) {
        const panel = panels[i];
        const dialogue = panel.dialogue?.trim();
        const speaker = panel.speaker?.trim();

        if (!dialogue) continue;

        const fullText = speaker ? `${speaker}: ${dialogue}` : dialogue;

        // Determine which page this panel belongs to
        const pageIdx = Math.floor(i / panelsPerPage);
        const panelInPage = i % panelsPerPage;

        // Calculate position within the page
        const row = Math.floor(panelInPage / panelCols);
        const col = panelInPage % panelCols;

        // Calculate absolute position in final image
        const pageX = pageIdx * pageWidth;
        const panelX = pageX + col * panelWidth;
        const panelY = row * panelHeight;

        const bubbleSvg = buildSpeechBubbleSvg({
            text: fullText,
            maxWidth: panelWidth - 30,
        });

        if (bubbleSvg) {
            composites.push({
                input: Buffer.from(bubbleSvg),
                left: Math.round(panelX + 15),
                top: Math.round(panelY + 15),
            });
        }
    }

    if (composites.length === 0) {
        return baseImageBuffer;
    }

    return await baseImage.composite(composites).png().toBuffer();
}

export async function generateComic({
    prompt,
    pages,
    panelsPerPage,
    style,
    requestId,
}) {
    // Step 1: Generate storyboard with Vietnamese dialogue using Gemini
    const { storyId, panels } = await runGeminiStoryboard({
        prompt,
        pages,
        panelsPerPage,
        requestId,
    });

    // Step 2: Generate comic layout using Nano Banana (English visual description)
    const output = await runNanaBanana({
        panels,
        pages,
        panelsPerPage,
        style,
        requestId,
    });

    const baseImageBuffer = await resolveOutputBuffer(output);

    // Step 3: Overlay Vietnamese speech bubbles on top of the comic image
    const finalImageBuffer = await overlayVietnameseBubbles({
        baseImageBuffer,
        panels,
        pages,
        panelsPerPage,
    });

    // Step 4: Upload final comic to R2
    // Use randomUUID to ensure unique key even with same prompt
    const comicId = randomUUID();
    const timestamp = Date.now();
    const key = `comics/${comicId}-${timestamp}/comic.png`;

    await uploadBufferToR2(finalImageBuffer, {
        key,
        contentType: "image/png",
    });

    const expiresIn = PERF.r2.presignExpiresSec || 3600;
    const presigned = await getImageUrl(key, expiresIn);
    const publicUrl = buildPublicUrl(key);

    logger.info(
        {
            requestId,
            comicId,
            panelCount: panels.length,
            model: { llm: GEMINI_MODEL, image: NANO_BANANA_MODEL },
        },
        "Generated comic with Vietnamese dialogue overlay"
    );

    return {
        comic_id: comicId,
        image: {
            key,
            url: publicUrl || presigned,
            presigned_url: presigned,
        },
        panels: panels.map((p) => ({
            id: p.id,
            description_vi: p.description_vi,
            dialogue: p.dialogue,
            speaker: p.speaker,
            emotion: p.emotion,
        })),
        meta: {
            pages,
            panelsPerPage,
            totalPanels: pages * panelsPerPage,
            model: {
                llm: GEMINI_MODEL,
                image: NANO_BANANA_MODEL,
            },
        },
    };
}
