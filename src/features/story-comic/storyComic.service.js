import { nanoid } from "nanoid";
import { replicate } from "../../integrations/replicate/client.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { withRetry } from "../../utils/retry.js";
import {
    uploadBufferToR2,
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import {
    generateOutlineWithGemini,
    generatePageStoryboard,
} from "./storyComic.llm.js";
import { renderComicPage } from "./storyComic.renderer.js";
import { PERF } from "../../config/perf.js";
import { logger } from "../../config/logger.js";

const LLM_MODEL = process.env.GEMINI_MODEL_ID || "google/gemini-2.5-flash";
const ANIMAGINE_MODEL =
    process.env.ANIMAGINE_MODEL_ID ||
    "cjwbw/animagine-xl-3.1:6afe2e6b27dad2d6f480b59195c221884b6acc589ff4d05ff0e5fc058690fbb9";

const DEFAULT_NEGATIVE_PROMPT =
    "nsfw, lowres, text, logo, watermark, signature, speech bubble, caption, bad hands, extra fingers, deformed, extra limbs";
const REQUIRED_TAGS = [
    "masterpiece",
    "best quality",
    "anime style",
    "vibrant colors",
    "high quality",
    "detailed background",
    "no text",
    "no speech bubble",
];
const BANNED_TAGS = ["manga", "screentone", "black and white"];

const FETCH_TIMEOUT_MS = Number(process.env.HTTP_TIMEOUT_MS || 20000);

function makeStoryId(prompt) {
    const slug = (prompt || "")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 32);
    return slug || `story-${nanoid(10)}`;
}

function normalizeEmotion(value) {
    const allowed = ["happy", "sad", "angry", "surprised", "neutral"];
    const lower = String(value || "").toLowerCase();
    return allowed.includes(lower) ? lower : "neutral";
}

function normalizeOutlineBeats(outline, pages, prompt) {
    const beats = Array.isArray(outline) ? outline : [];
    const sanitized = beats.map((b, idx) => ({
        id: Number.isFinite(b?.id) ? b.id : idx + 1,
        summary_vi:
            (b?.summary_vi || "").toString().trim() ||
            `Diễn biến ${idx + 1}: ${prompt}`,
        summary_en:
            (b?.summary_en || "").toString().trim() ||
            `Beat ${idx + 1} of ${prompt}`,
        main_emotion: normalizeEmotion(b?.main_emotion),
    }));

    const minBeats = pages * 3;
    const maxBeats = 12;
    while (sanitized.length < minBeats) {
        const idx = sanitized.length;
        sanitized.push({
            id: idx + 1,
            summary_vi: `Diễn biến ${idx + 1}: ${prompt}`,
            summary_en: `Beat ${idx + 1} of ${prompt}`,
            main_emotion: "neutral",
        });
    }

    return sanitized.slice(0, maxBeats);
}

function splitOutline(outline, pages) {
    const beatsPerPage = Math.max(
        3,
        Math.min(4, Math.ceil(outline.length / pages))
    );
    const chunks = [];
    let cursor = 0;
    for (let i = 0; i < pages; i++) {
        const next = outline.slice(cursor, cursor + beatsPerPage);
        cursor += beatsPerPage;
        chunks.push(next.length ? next : outline.slice(-beatsPerPage));
    }
    return chunks;
}

function normalizePromptTags(raw) {
    const tags = String(raw || "")
        .split(/[,|]/)
        .map((t) => t.trim())
        .filter(
            (t) =>
                t &&
                !BANNED_TAGS.some((ban) =>
                    t.toLowerCase().includes(ban.toLowerCase())
                )
        );

    REQUIRED_TAGS.forEach((req) => {
        if (!tags.some((t) => t.toLowerCase() === req.toLowerCase())) {
            tags.push(req);
        }
    });

    const seen = new Set();
    const unique = [];
    for (const t of tags) {
        const key = t.toLowerCase();
        if (seen.has(key)) continue;
        seen.add(key);
        unique.push(t);
    }

    return unique.join(", ");
}

function normalizePanels(panels, panelsPerPage, beats, prompt) {
    const safe = Array.isArray(panels) ? panels.slice(0, panelsPerPage) : [];
    const result = [];
    for (let i = 0; i < panelsPerPage; i++) {
        const panel = safe[i] || {};
        const beat = beats[i % beats.length] || {};
        result.push({
            id: Number.isFinite(panel.id) ? panel.id : i + 1,
            description_vi:
                (panel.description_vi || "").toString().trim() ||
                beat.summary_vi ||
                `Cảnh ${i + 1}: ${prompt}`,
            prompt_tags: normalizePromptTags(panel.prompt_tags),
            dialogue: (panel.dialogue || "").toString().trim(),
            speaker: (panel.speaker || "Narrator").toString().trim(),
            emotion: normalizeEmotion(panel.emotion),
        });
    }
    return result;
}

function buildImagePrompt(panel) {
    return normalizePromptTags(panel.prompt_tags);
}

async function fetchBuffer(url, timeoutMs = FETCH_TIMEOUT_MS) {
    const downloadOnce = async () => {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);
        try {
            const resp = await fetch(url, { signal: controller.signal });
            if (!resp.ok) throw new Error(`Fetch failed ${resp.status}`);
            const arr = await resp.arrayBuffer();
            return Buffer.from(arr);
        } finally {
            clearTimeout(timer);
        }
    };

    try {
        return await withRetry(downloadOnce, {
            retries: 2,
            baseDelayMs: 800,
            factor: 2,
        });
    } catch (err) {
        throw new Error(`Download panel image failed: ${err?.message || err}`);
    }
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
        const arr = await blob.arrayBuffer();
        return Buffer.from(arr);
    }
    if (first?.url) {
        return await fetchBuffer(first.url);
    }
    if (Buffer.isBuffer(first)) return first;
    return Buffer.from(first);
}

async function runAnimagine({ prompt, seed, requestId }) {
    const retryOpts = {
        retries: PERF.retry?.retries ?? 2,
        baseDelayMs: PERF.retry?.minTimeoutMs ?? 600,
        factor: PERF.retry?.factor ?? 2,
    };

    return withRetry(
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
                        ...(Number.isFinite(seed) ? { seed } : {}),
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

async function generatePanelsImages(panels, { pageIndex, requestId }) {
    const tasks = panels.map(async (panel, idx) => {
        const prompt = buildImagePrompt(panel);
        const seed = Number.isFinite(pageIndex)
            ? pageIndex * 100 + idx
            : undefined;
        const output = await runAnimagine({ prompt, seed, requestId });
        const buffer = await resolveOutputBuffer(output);
        return {
            ...panel,
            imageBuffer: buffer,
        };
    });

    return Promise.all(tasks);
}

export async function generateStoryComic({
    prompt,
    pages,
    panelsPerPage,
    requestId,
}) {
    const outlineResp = await generateOutlineWithGemini({
        prompt,
        pages,
        requestId,
    });

    const storyId = outlineResp.story_id || makeStoryId(prompt);
    const outline = normalizeOutlineBeats(outlineResp.outline, pages, prompt);
    const outlineChunks = splitOutline(outline, pages);

    const results = [];
    const metaPages = [];

    for (let pageIndex = 0; pageIndex < outlineChunks.length; pageIndex++) {
        const beats = outlineChunks[pageIndex];
        const storyboard = await generatePageStoryboard({
            beats,
            pageIndex,
            panels: panelsPerPage,
            storyId,
            requestId,
        });
        const normalizedPanels = normalizePanels(
            storyboard.panels,
            panelsPerPage,
            beats,
            prompt
        );
        const renderedPanels = await generatePanelsImages(normalizedPanels, {
            pageIndex,
            requestId,
        });
        const pageBuffer = await renderComicPage({
            storyId,
            pageIndex,
            panels: renderedPanels,
        });

        const key = `comics/${storyId}/page-${pageIndex}.png`;
        await uploadBufferToR2(pageBuffer, {
            key,
            contentType: "image/png",
        });
        const expiresIn = PERF.r2.presignExpiresSec || 3600;
        const presignedUrl = await presignGetUrl(key, expiresIn);
        const publicUrl = buildPublicUrl(key);

        results.push({
            page_index: pageIndex,
            page_url: publicUrl || presignedUrl,
            key,
            presigned_url: presignedUrl,
            panels: renderedPanels.map((p) => ({
                id: p.id,
                dialogue: p.dialogue,
                speaker: p.speaker,
                emotion: p.emotion,
            })),
        });

        metaPages.push({
            page_index: pageIndex,
            beats: beats.map((b) => b.id),
            panel_count: renderedPanels.length,
        });
    }

    logger.info(
        {
            requestId,
            storyId,
            pages: results.length,
            model: { llm: LLM_MODEL, image: ANIMAGINE_MODEL },
        },
        "Generated story comic (multi-page)"
    );

    return {
        storyId,
        pages: results,
        meta: {
            outline,
            pages: metaPages,
            model: { llm: LLM_MODEL, image: ANIMAGINE_MODEL },
        },
    };
}
