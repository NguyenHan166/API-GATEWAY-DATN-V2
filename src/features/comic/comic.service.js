import { randomUUID } from "crypto";
import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { withRetry } from "../../utils/retry.js";
import {
    uploadBufferToR2,
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { PERF } from "../../config/perf.js";
import { logger } from "../../config/logger.js";

const GEMINI_MODEL = process.env.GEMINI_MODEL_ID || "google/gemini-2.5-flash";
const ANIMAGINE_MODEL =
    process.env.ANIMAGINE_MODEL_ID ||
    "cjwbw/animagine-xl-3.1:6afe2e6b27dad2d6f480b59195c221884b6acc589ff4d05ff0e5fc058690fbb9";

const DEFAULT_NEGATIVE_PROMPT =
    "nsfw, lowres, text, logo, watermark, signature, speech bubble, caption, bad hands, extra fingers, deformed, extra limbs";
const POSITIVE_SUFFIX =
    "anime style, vibrant colors, high quality, detailed background, no text, no speech bubble";

const PAGE_WIDTH = 1080;
const PAGE_HEIGHT = 1620;
const GAP = 18;
const FETCH_TIMEOUT_MS = Number(process.env.HTTP_TIMEOUT_MS || 20000);

function buildSystemPrompt({ panels }) {
    return `
Bạn là trợ lý tạo storyboard comic anime màu (không manga).
- Trả về đúng 1 object JSON, không markdown, không giải thích, không kèm text thừa.
- JSON phải bắt đầu bằng { và kết thúc bằng }.
- Phải có đúng ${panels} panel trong mảng "panels".
- Mỗi panel cần: id, description_vi (tiếng Việt, 1 câu gọn ≤ 25 từ, nêu cảnh, ánh sáng, mood), prompt_tags (Danbooru tiếng Anh, đủ tag nhân vật/cảnh/background/chất liệu ánh sáng), dialogue (tiếng Việt tự nhiên ≤ 40 ký tự), speaker, emotion (happy|sad|angry|surprised|neutral).
- Không dùng từ khóa manga, screentone, black and white, lineart only.
- prompt_tags phải hợp với Animagine XL 3.1: masterpiece, best quality, anime style, vibrant colors, high quality, detailed background, no text, no speech bubble, [các tag cảnh/nhân vật], ánh sáng (soft lighting/moody lighting), góc máy (dynamic angle/close up/...).
- Dùng template JSON hợp lệ:
{
  "story_id": "story-id",
  "characters": [
    {"name": "Tên", "role": "main|support", "description_en": "english tags of appearance"}
  ],
  "panels": [
    {
      "id": 1,
      "description_vi": "string",
      "prompt_tags": "tag1, tag2, ...",
      "dialogue": "string",
      "speaker": "string",
      "emotion": "happy|sad|angry|surprised|neutral"
    }
  ]
}
    `.trim();
}

function buildUserPrompt({ userPrompt, style }) {
    return `
Người dùng muốn: ${userPrompt}
Style trang: ${style || "anime_color"}
Tạo storyboard JSON với thoại ngắn, giữ nguyên bối cảnh nhân vật, dùng tiếng Việt tự nhiên.
    `.trim();
}

function buildFallbackStoryboard({ prompt, panels, storyId }) {
    return {
        story_id: storyId,
        panels: Array.from({ length: panels }).map((_, i) => ({
            id: i + 1,
            description_vi: `Khung ${i + 1}: ${prompt}`,
            prompt_tags:
                "masterpiece, best quality, anime style, vibrant colors, detailed background, no text, no speech bubble",
            dialogue: " ",
            speaker: "Narrator",
            emotion: "neutral",
        })),
        characters: [],
    };
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

    const direct = rawStr.trim();
    try {
        return JSON.parse(direct);
    } catch (_) {}

    const match = rawStr.match(/\{[\s\S]*\}/);
    if (match) {
        try {
            return JSON.parse(match[0]);
        } catch (_) {}
    }
    throw new Error("Model did not return valid JSON");
}

function normalizePanels(panels, requested, fallbackPrompt) {
    const safe = Array.isArray(panels) ? panels.slice(0, requested) : [];
    const filled = [];
    for (let i = 0; i < requested; i++) {
        const p = safe[i] || {};
        const dialogue = p.dialogue || " ";
        filled.push({
            id: p.id || i + 1,
            description_vi:
                p.description_vi ||
                `Khung ${i + 1}: ${fallbackPrompt || "Cảnh anime màu."}`,
            prompt_tags:
                p.prompt_tags ||
                "masterpiece, best quality, anime style, vibrant colors, detailed background, no text, no speech bubble",
            dialogue,
            speaker: p.speaker || "Narrator",
            emotion: p.emotion || "neutral",
        });
    }
    return filled.map((p, idx) => ({ ...p, id: p.id || idx + 1 }));
}

function buildPanelPrompt(panel) {
    const tags = panel.prompt_tags || "";
    const trimmed = [tags, POSITIVE_SUFFIX]
        .filter(Boolean)
        .join(", ")
        .replace(/\s+,/g, ",")
        .replace(/,+/g, ",")
        .replace(/,\s*,/g, ",");

    return trimmed;
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

async function runGeminiStoryboard({ prompt, panels, style, requestId }) {
    const systemPrompt = buildSystemPrompt({ panels });
    const userPrompt = buildUserPrompt({ userPrompt: prompt, style });
    const finalPrompt = `
${systemPrompt}

YÊU CẦU ĐẦU RA:
- Trả về đúng 1 object JSON, không markdown, không giải thích, không kèm chữ thừa.
- JSON có mảng "panels" độ dài ${panels}.
- Mỗi panel: { "id", "description_vi", "prompt_tags", "dialogue", "speaker", "emotion" }.
- Thoại tiếng Việt ≤ 40 ký tự, tự nhiên, đúng nhân vật. Trong prompt_tags phải có bối cảnh, ánh sáng, góc máy, màu sắc.

NỘI DUNG NGƯỜI DÙNG:
${userPrompt}
    `.trim();

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
                        prompt: finalPrompt,
                        max_output_tokens: 5000,
                        temperature: 0.25,
                    },
                })
            ),
        retryOpts
    );

    const storyId = randomUUID();
    let parsed;
    try {
        parsed = extractJson(llmOutput);
    } catch (err) {
        logger.warn(
            {
                requestId,
                model: GEMINI_MODEL,
                err: err?.message,
                raw: String(llmOutput)?.slice(0, 300),
            },
            "Gemini returned invalid JSON, using fallback storyboard"
        );
        parsed = buildFallbackStoryboard({ prompt, panels, storyId });
    }

    const story = {
        story_id: parsed.story_id || storyId,
        panels: normalizePanels(parsed.panels, panels, prompt),
        characters: Array.isArray(parsed.characters) ? parsed.characters : [],
    };

    logger.info(
        { requestId, storyId: story.story_id, model: GEMINI_MODEL },
        "Generated comic storyboard (inline)"
    );

    return story;
}

async function runAnimagine({ prompt, requestId }) {
    const retryOpts = {
        retries: PERF.retry.retries || 2,
        baseDelayMs: PERF.retry.minTimeoutMs || 600,
        factor: PERF.retry.factor || 2,
    };

    return await withRetry(
        () =>
            withReplicateLimiter(() =>
                replicate.run(ANIMAGINE_MODEL, {
                    input: {
                        prompt,
                        negative_prompt: DEFAULT_NEGATIVE_PROMPT,
                        width: 832,
                        height: 1216,
                        num_inference_steps: 28,
                        guidance_scale: 7,
                    },
                })
            ),
        {
            ...retryOpts,
            beforeRetry: ({ attempt }) =>
                logger.warn(
                    { attempt, model: ANIMAGINE_MODEL, requestId },
                    "Retrying Animagine generation"
                ),
        }
    );
}

async function generatePanelsImages(panels, requestId) {
    const tasks = panels.map(async (panel) => {
        const prompt = buildPanelPrompt(panel);
        const output = await runAnimagine({ prompt, requestId });
        const first =
            Array.isArray(output) && output.length > 0
                ? output[0]
                : output?.output || output;
        if (!first) throw new Error("Model returned empty output");

        let buf;
        if (typeof first === "string") {
            buf = await fetchBuffer(first, FETCH_TIMEOUT_MS);
        } else if (typeof first?.blob === "function") {
            const blob = await first.blob();
            const ab = await blob.arrayBuffer();
            buf = Buffer.from(ab);
        } else if (first?.url) {
            buf = await fetchBuffer(first.url, FETCH_TIMEOUT_MS);
        } else if (Buffer.isBuffer(first)) {
            buf = first;
        } else {
            buf = Buffer.from(first);
        }

        const pngBuffer = await sharp(buf).png().toBuffer();
        return { ...panel, imageBuffer: pngBuffer };
    });

    return await Promise.all(tasks);
}

function wrapText(text, maxChars) {
    if (!text) return [""];
    const words = text.split(/\s+/);
    const lines = [];
    let current = "";
    for (const w of words) {
        if ((current + " " + w).trim().length > maxChars) {
            if (current) lines.push(current.trim());
            current = w;
        } else {
            current = `${current} ${w}`.trim();
        }
    }
    if (current) lines.push(current.trim());
    return lines.length ? lines : [""];
}

function makeSpeechBubble({
    dialogue,
    speaker,
    maxWidth,
    maxHeight,
    fontSize = 18,
}) {
    const combined = speaker ? `${speaker}: ${dialogue || ""}` : dialogue || "";
    const safeWidth = Math.max(220, Math.min(maxWidth - 12, 520));
    const lineHeight = fontSize + 6;
    const maxChars = Math.max(14, Math.floor(safeWidth / 9));
    const potentialLines = wrapText(combined, maxChars);
    const maxLines = Math.max(2, Math.floor(maxHeight / lineHeight) - 1);
    const lines =
        potentialLines.length > maxLines
            ? [
                  ...potentialLines.slice(0, maxLines - 1),
                  `${potentialLines[maxLines - 1]}...`,
              ]
            : potentialLines;

    const padding = 12;
    const tailHeight = 14;
    const bodyHeight = padding * 2 + lines.length * lineHeight;
    const bubbleHeight = Math.min(bodyHeight + tailHeight, maxHeight);

    const textYStart = padding + fontSize;
    const text = lines
        .map(
            (line, idx) =>
                `<text x="${padding}" y="${
                    textYStart + idx * lineHeight
                }" font-size="${fontSize}" font-family="Arial, sans-serif" fill="#000">${line.replace(
                    /&/g,
                    "&amp;"
                )}</text>`
        )
        .join("");

    const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="${safeWidth}" height="${bubbleHeight}">
  <path d="M8 8 h ${safeWidth - 16} a8 8 0 0 1 8 8 v ${
        bubbleHeight - tailHeight - 16
    } a8 8 0 0 1 -8 8 h -${safeWidth - 16} a8 8 0 0 1 -8 -8 v -${
        bubbleHeight - tailHeight - 16
    } a8 8 0 0 1 8 -8 z" fill="white" stroke="black" stroke-width="2" />
  <path d="M${safeWidth * 0.15} ${bubbleHeight - tailHeight} l 18 ${
        tailHeight - 2
    } l -8 -${tailHeight}" fill="white" stroke="black" stroke-width="2" />
  ${text}
</svg>
    `.trim();

    return Buffer.from(svg);
}

function buildLayout(count) {
    if (count <= 0) return [];
    // Simple grid layout; keeps ratio pleasant for 1-6 panels
    if (count === 1)
        return [
            {
                x: GAP,
                y: GAP,
                width: PAGE_WIDTH - 2 * GAP,
                height: PAGE_HEIGHT - 2 * GAP,
            },
        ];
    if (count === 2)
        return [
            {
                x: GAP,
                y: GAP,
                width: (PAGE_WIDTH - 3 * GAP) / 2,
                height: PAGE_HEIGHT - 2 * GAP,
            },
            {
                x: GAP * 2 + (PAGE_WIDTH - 3 * GAP) / 2,
                y: GAP,
                width: (PAGE_WIDTH - 3 * GAP) / 2,
                height: PAGE_HEIGHT - 2 * GAP,
            },
        ];
    const rows = Math.ceil(Math.sqrt(count));
    const cols = Math.ceil(count / rows);
    const cellW = Math.floor((PAGE_WIDTH - GAP * (cols + 1)) / cols);
    const cellH = Math.floor((PAGE_HEIGHT - GAP * (rows + 1)) / rows);

    const rects = [];
    let idx = 0;
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            if (idx >= count) break;
            const x = GAP + c * (cellW + GAP);
            const y = GAP + r * (cellH + GAP);
            rects.push({ x, y, width: cellW, height: cellH });
            idx++;
        }
    }
    return rects;
}

async function composePage({ storyId, renderedPanels }) {
    const layout = buildLayout(renderedPanels.length);
    const composites = [];

    renderedPanels.forEach((p, idx) => {
        const cell = layout[idx] || layout[layout.length - 1];
        composites.push({
            input: sharp(p.imageBuffer)
                .resize(cell.width, cell.height, { fit: "cover" })
                .png()
                .toBuffer(),
            left: Math.round(cell.x),
            top: Math.round(cell.y),
        });

        const hasDialogue = (p.dialogue || "").trim().length > 0;
        if (hasDialogue) {
            const bubble = makeSpeechBubble({
                dialogue: p.dialogue,
                speaker: p.speaker,
                maxWidth: cell.width,
                maxHeight: Math.floor(cell.height * 0.45),
            });
            composites.push({
                input: bubble,
                left: Math.round(cell.x + 8),
                top: Math.round(cell.y + 8),
            });
        }
    });

    const resolvedComposites = await Promise.all(
        composites.map(async (c) => ({
            input: await c.input,
            left: c.left,
            top: c.top,
        }))
    );

    const pageBuffer = await sharp({
        create: {
            width: PAGE_WIDTH,
            height: PAGE_HEIGHT,
            channels: 4,
            background: { r: 28, g: 28, b: 28, alpha: 1 },
        },
    })
        .composite(resolvedComposites)
        .png()
        .toBuffer();

    const key = `comics/${storyId}/page-0.png`;
    await uploadBufferToR2(pageBuffer, {
        key,
        contentType: "image/png",
    });
    const expiresIn = PERF.r2.presignExpiresSec || 3600;
    const presigned = await presignGetUrl(key, expiresIn);
    const publicUrl = buildPublicUrl(key);

    return {
        key,
        url: publicUrl || presigned,
        presigned_url: presigned,
        buffer: pageBuffer,
    };
}

export async function generateComic({ prompt, panels, style, requestId }) {
    const story = await runGeminiStoryboard({
        prompt,
        panels,
        style,
        requestId,
    });
    const renderedPanels = await generatePanelsImages(story.panels, requestId);
    const page = await composePage({
        storyId: story.story_id,
        renderedPanels,
    });

    logger.info(
        { requestId, storyId: story.story_id, model: ANIMAGINE_MODEL },
        "Rendered comic page (single API)"
    );

    return {
        page,
        story_id: story.story_id,
        meta: {
            panels: renderedPanels.map((p) => ({
                id: p.id,
                dialogue: p.dialogue,
                speaker: p.speaker,
                emotion: p.emotion,
            })),
            model: {
                llm: GEMINI_MODEL,
                image: ANIMAGINE_MODEL,
            },
        },
    };
}
