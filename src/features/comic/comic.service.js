import { randomUUID } from "crypto";
import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { withRetry } from "../../utils/retry.js";
import {
    uploadBufferToR2,
    presignGetUrl,
    buildPublicUrl,
    getImageUrl,
} from "../../integrations/r2/storage.service.js";
import { PERF } from "../../config/perf.js";
import { logger } from "../../config/logger.js";
import { renderComicPage } from "../story-comic/storyComic.renderer.js";

const GEMINI_MODEL = process.env.GEMINI_MODEL_ID || "google/gemini-2.5-flash";
const ANIMAGINE_MODEL =
    process.env.ANIMAGINE_MODEL_ID ||
    "cjwbw/animagine-xl-3.1:6afe2e6b27dad2d6f480b59195c221884b6acc589ff4d05ff0e5fc058690fbb9";

const DEFAULT_NEGATIVE_PROMPT =
    "nsfw, lowres, text, logo, watermark, signature, speech bubble, caption, bad hands, extra fingers, deformed, extra limbs";
const POSITIVE_SUFFIX =
    "anime style, vibrant colors, high quality, detailed background, no text, no speech bubble";

const FETCH_TIMEOUT_MS = Number(process.env.HTTP_TIMEOUT_MS || 20000);

function detectMainGender(text) {
    const t = (text || "").toLowerCase();
    const maleKeywords = [
        "chàng trai",
        "anh ",
        "anh ấy",
        "anh ay",
        "người đàn ông",
        "nguoi dan ong",
        "chàng thanh niên",
        "chang thanh nien",
        "nam chính",
        "nam chinh",
        "cậu bé",
        "cau be",
        "chàng",
    ];
    const femaleKeywords = [
        "cô gái",
        "co gai",
        "cô bé",
        "co be",
        "người phụ nữ",
        "nguoi phu nu",
        "nữ chính",
        "nu chinh",
        "cô nàng",
        "co nang",
        "chị ",
        "chi ",
    ];
    if (maleKeywords.some((k) => t.includes(k))) return "male";
    if (femaleKeywords.some((k) => t.includes(k))) return "female";
    return null;
}

function buildSystemPrompt({ panels, characterHint }) {
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
- Giữ 1 nhân vật chính xuyên suốt, cùng giới tính${
        characterHint ? ` (${characterHint})` : ""
    }, trang phục và kiểu tóc nhất quán ở mọi panel.
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

function applyGenderTags(tags, gender) {
    if (!gender) return tags;
    const lower = tags.toLowerCase();
    if (gender === "male") {
        if (
            !lower.includes("male") &&
            !lower.includes("1boy") &&
            !lower.includes("man")
        ) {
            return `${tags}, male, 1boy, masculine face, short hair`;
        }
    }
    if (gender === "female") {
        if (
            !lower.includes("female") &&
            !lower.includes("1girl") &&
            !lower.includes("woman")
        ) {
            return `${tags}, female, 1girl, feminine face`;
        }
    }
    return tags;
}

function buildPanelPrompt(panel, mainGender) {
    const tags = panel.prompt_tags || "";
    const gendered = applyGenderTags(tags, mainGender);
    const trimmed = [gendered, POSITIVE_SUFFIX]
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

async function runGeminiStoryboard({
    prompt,
    panels,
    style,
    requestId,
    mainGender,
}) {
    const genderHint =
        mainGender === "male" ? "nam" : mainGender === "female" ? "nữ" : null;
    const systemPrompt = buildSystemPrompt({
        panels,
        characterHint: genderHint,
    });
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

async function generatePanelsImages(panels, requestId, mainGender) {
    const tasks = panels.map(async (panel) => {
        const prompt = buildPanelPrompt(panel, mainGender);
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

async function composePage({ storyId, renderedPanels }) {
    const pageBuffer = await renderComicPage({
        storyId,
        pageIndex: 0,
        panels: renderedPanels,
    });

    const key = `comics/${storyId}/page-0.png`;
    await uploadBufferToR2(pageBuffer, {
        key,
        contentType: "image/png",
    });
    const expiresIn = PERF.r2.presignExpiresSec || 3600;
    const presigned = await getImageUrl(key, expiresIn);
    const publicUrl = buildPublicUrl(key);

    return {
        key,
        url: publicUrl || presigned,
        presigned_url: presigned,
        buffer: pageBuffer,
    };
}

export async function generateComic({ prompt, panels, style, requestId }) {
    const mainGender = detectMainGender(prompt);
    const story = await runGeminiStoryboard({
        prompt,
        panels,
        style,
        requestId,
        mainGender,
    });
    const renderedPanels = await generatePanelsImages(
        story.panels,
        requestId,
        mainGender
    );
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
