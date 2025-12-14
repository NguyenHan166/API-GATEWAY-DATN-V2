import { nanoid } from "nanoid";
import { replicate } from "../../integrations/replicate/client.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { withRetry } from "../../utils/retry.js";
import { PERF } from "../../config/perf.js";
import { logger } from "../../config/logger.js";

const GEMINI_MODEL = process.env.GEMINI_MODEL_ID || "google/gemini-2.5-flash";

const RETRY_OPTS = {
    retries: PERF.retry?.retries ?? 2,
    baseDelayMs: PERF.retry?.minTimeoutMs ?? 600,
    factor: PERF.retry?.factor ?? 2,
};

function sanitizeStoryId(raw, fallbackPrompt) {
    const base =
        raw ||
        fallbackPrompt
            ?.toLowerCase()
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "")
            .slice(0, 32) ||
        `story-${nanoid(10)}`;
    const cleaned = base.replace(/[^a-z0-9-]/g, "");
    return cleaned || `story-${nanoid(10)}`;
}

function extractJson(raw) {
    if (raw === null || raw === undefined) return null;
    const pieces = [];
    if (typeof raw === "string") pieces.push(raw);
    if (Array.isArray(raw)) pieces.push(raw.join(""));
    if (raw?.output) pieces.push(raw.output);

    const joined = pieces
        .filter(Boolean)
        .map((x) => (Array.isArray(x) ? x.join("") : String(x)))
        .join("\n")
        .trim();
    if (!joined) return null;

    try {
        return JSON.parse(joined);
    } catch (_) {}

    const match = joined.match(/\{[\s\S]*\}/);
    if (match) {
        try {
            return JSON.parse(match[0]);
        } catch (_) {}
    }
    return null;
}

async function repairJson(raw, { requestId, context }) {
    const text = typeof raw === "string" ? raw : JSON.stringify(raw);
    const fixPrompt = `
Đoạn sau cần được chuyển thành JSON hợp lệ, đúng schema, không markdown:
${text}

Yêu cầu:
- Chỉ trả về JSON, không giải thích.
- Giữ nguyên ngữ nghĩa, chỉ sửa cú pháp JSON.
`.trim();

    try {
        const fixed = await withRetry(
            () =>
                withReplicateLimiter(() =>
                    replicate.run(GEMINI_MODEL, {
                        input: {
                            prompt: fixPrompt,
                            max_output_tokens: 3000,
                            temperature: 0.1,
                        },
                    })
                ),
            RETRY_OPTS
        );
        return extractJson(fixed);
    } catch (err) {
        logger.warn(
            { requestId, context, err: err?.message },
            "Failed to repair JSON with Gemini"
        );
        return null;
    }
}

async function callGemini(prompt, { temperature = 0.3, maxTokens = 5000 }) {
    return withRetry(
        () =>
            withReplicateLimiter(() =>
                replicate.run(GEMINI_MODEL, {
                    input: {
                        prompt,
                        max_output_tokens: maxTokens,
                        temperature,
                    },
                })
            ),
        RETRY_OPTS
    );
}

function buildOutlinePrompt({ userPrompt, pages }) {
    const beatHint = pages === 2 ? "8-9" : "9-12";
    return `
Bạn là biên kịch truyện tranh anime màu. Tạo outline chi tiết cho câu chuyện từ prompt của người dùng, gồm ${beatHint} beat (mở đầu -> cao trào -> kết thúc) để vẽ ${pages} trang comic (mỗi trang 3-4 panel).

QUAN TRỌNG - NHÂN VẬT NHẤT QUÁN:
- Tạo danh sách nhân vật (characters) với mô tả chi tiết bằng Danbooru tags tiếng Anh.
- Mỗi nhân vật cần: appearance_tags (ngoại hình cố định), outfit_tags (trang phục), distinguishing_features (đặc điểm nhận dạng).
- Các tags này sẽ được dùng cho MỌI panel để đảm bảo nhân vật giống nhau xuyên suốt truyện.

Yêu cầu khác:
- Viết tiếng Việt mạch lạc cho summary_vi, thêm summary_en 1-2 câu tiếng Anh, chọn main_emotion (happy|sad|angry|surprised|neutral).
- story_id ngắn, không dấu, không khoảng trắng.
- Chỉ trả về JSON object, không markdown, không kèm chữ thừa.

Schema JSON:
{
  "story_id": "string_ngan_khong_dau_cach",
  "characters": [
    {
      "name": "Tên nhân vật (tiếng Việt hoặc Nhật)",
      "role": "main hoặc supporting",
      "appearance_tags": "1girl/1boy, hair color, hair length, eye color, skin tone, body type (Danbooru tags)",
      "outfit_tags": "clothing details, accessories (Danbooru tags)",
      "distinguishing_features": "unique features like hairpin, glasses, scar (Danbooru tags)"
    }
  ],
  "outline": [
    {
      "id": 1,
      "summary_vi": "mô tả ngắn gọn tiếng Việt (~1–2 câu)",
      "summary_en": "short English description",
      "main_emotion": "happy|sad|angry|surprised|neutral"
    }
  ]
}

PROMPT NGƯỜI DÙNG:
${userPrompt}
`.trim();
}

function buildStoryboardPrompt({ beats, pageIndex, panels, storyId }) {
    const beatLines = beats
        .map(
            (b, idx) =>
                `- Beat ${b.id || idx + 1}: ${b.summary_vi || ""} (emotion: ${
                    b.main_emotion || "neutral"
                })`
        )
        .join("\n");

    return `
Bạn là biên kịch storyboard cho truyện tranh anime màu, không phải manga. Tạo storyboard JSON cho trang #${
        pageIndex + 1
    } của truyện ${storyId}.
- Trang có ${panels} panel, kiểu anime màu, tỉ lệ 2:3.
- Nhân vật chính phải nhất quán giữa các panel (giới tính, trang phục, mái tóc); nếu có mèo phép thuật thì giữ đúng là mèo phép thuật, không đổi loài/giới tính.
- prompt_tags phải ở dạng Danbooru tiếng Anh, ngắn gọn, chứa bắt buộc: "masterpiece, best quality, anime style, vibrant colors, detailed background, no text, no speech bubble". Không được chứa: "manga", "screentone", "black and white".
- Mỗi dialogue tiếng Việt 1-2 câu, tự nhiên, ≤ 70 ký tự, speaker là tên nhân vật (giữ tên lặp lại nhất quán), bám sát cảm xúc trong beat và bối cảnh.
- Chỉ trả về JSON, không markdown, không giải thích.

Context từ outline:
${beatLines}

Schema JSON:
{
  "page_index": ${pageIndex},
  "size": "2:3",
  "style": "anime_color",
  "panels": [
    {
      "id": 1,
      "description_vi": "mô tả tiếng Việt 1-2 câu",
      "prompt_tags": "tag1, tag2, ...",
      "dialogue": "lời thoại tiếng Việt ngắn",
      "speaker": "tên nhân vật",
      "emotion": "happy|sad|angry|surprised|neutral"
    }
  ],
  "beats_used": [${beats.map((b) => b.id).join(", ")}]
}
`.trim();
}

function fallbackCharacter(prompt) {
    return [
        {
            name: "Main Character",
            role: "main",
            appearance_tags: "1girl, long black hair, brown eyes, fair skin",
            outfit_tags: "casual clothes, white shirt, blue jeans",
            distinguishing_features: "gentle expression",
        },
    ];
}

function normalizeCharacters(characters, prompt) {
    if (!Array.isArray(characters) || characters.length === 0) {
        return fallbackCharacter(prompt);
    }
    return characters.map((char) => ({
        name: (char.name || "Character").toString().trim(),
        role: ["main", "supporting"].includes(char.role) ? char.role : "main",
        appearance_tags: (char.appearance_tags || "1girl").toString().trim(),
        outfit_tags: (char.outfit_tags || "casual clothes").toString().trim(),
        distinguishing_features: (char.distinguishing_features || "")
            .toString()
            .trim(),
    }));
}

function fallbackOutline({ prompt, pages }) {
    const beatsCount = pages === 2 ? 8 : 10;
    const storyId = sanitizeStoryId(null, prompt);
    const characters = fallbackCharacter(prompt);
    const outline = Array.from({ length: beatsCount }).map((_, idx) => ({
        id: idx + 1,
        summary_vi: `Diễn biến ${idx + 1} của câu chuyện: ${prompt}`,
        summary_en: `Beat ${idx + 1} of the story about ${prompt}`,
        main_emotion: "neutral",
    }));
    return { story_id: storyId, characters, outline };
}

function fallbackStoryboard({ beats, pageIndex, panels, storyId }) {
    return {
        page_index: pageIndex,
        size: "2:3",
        style: "anime_color",
        beats_used: beats.map((b) => b.id || 0),
        panels: Array.from({ length: panels }).map((_, idx) => ({
            id: idx + 1,
            description_vi: `Cảnh ${idx + 1} trang ${pageIndex + 1}: ${
                beats[idx % beats.length]?.summary_vi || ""
            }`,
            prompt_tags:
                "masterpiece, best quality, anime style, vibrant colors, detailed background, no text, no speech bubble",
            dialogue: "",
            speaker: "Narrator",
            emotion: "neutral",
        })),
    };
}

export async function generateOutlineWithGemini({ prompt, pages, requestId }) {
    const outlinePrompt = buildOutlinePrompt({ userPrompt: prompt, pages });
    const raw = await callGemini(outlinePrompt, {
        temperature: 0.25,
        maxTokens: 6000,
    });

    let parsed = extractJson(raw);
    if (!parsed) {
        parsed = await repairJson(raw, { requestId, context: "outline" });
    }
    if (!parsed) {
        logger.warn(
            { requestId, model: GEMINI_MODEL },
            "Gemini returned invalid outline, using fallback"
        );
        return fallbackOutline({ prompt, pages });
    }

    const storyId = sanitizeStoryId(parsed.story_id, prompt);
    const outline = Array.isArray(parsed.outline) ? parsed.outline : [];
    const characters = normalizeCharacters(parsed.characters, prompt);

    logger.info(
        { requestId, storyId, characterCount: characters.length },
        "Generated outline with character sheet"
    );

    return {
        story_id: storyId,
        characters,
        outline,
    };
}

export async function generatePageStoryboard({
    beats,
    pageIndex,
    panels,
    storyId,
    requestId,
}) {
    const sbPrompt = buildStoryboardPrompt({
        beats,
        pageIndex,
        panels,
        storyId,
    });
    const raw = await callGemini(sbPrompt, {
        temperature: 0.28,
        maxTokens: 5000,
    });

    let parsed = extractJson(raw);
    if (!parsed) {
        parsed = await repairJson(raw, { requestId, context: "storyboard" });
    }
    if (!parsed) {
        logger.warn(
            { requestId, pageIndex, model: GEMINI_MODEL },
            "Gemini returned invalid storyboard, using fallback"
        );
        return fallbackStoryboard({ beats, pageIndex, panels, storyId });
    }

    const panelsArr = Array.isArray(parsed.panels) ? parsed.panels : [];
    const usedBeats = Array.isArray(parsed.beats_used)
        ? parsed.beats_used
        : beats.map((b) => b.id || 0);

    return {
        page_index: parsed.page_index ?? pageIndex,
        size: parsed.size || "2:3",
        style: parsed.style || "anime_color",
        beats_used: usedBeats,
        panels: panelsArr,
    };
}
