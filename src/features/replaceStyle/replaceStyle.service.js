import sharp from "sharp";
import { PERF } from "../../config/perf.js";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";

// Model FLUX Kontext (dev) — I2I
const MODEL = "black-forest-labs/flux-kontext-dev";

// Preset prompt cho từng style (giữ bố cục/khuôn mặt)
const STYLE_PRESETS = {
    anime: "Convert the whole image to anime cel-shaded style with clean ink outlines and flat 2–3 tone shading. Preserve the original color palette (skin, hair, clothing, background) with only minimal hue shift; keep natural skin tones. Keep the original face, pose and composition; eyes with specular highlights.",
    ghibli: "Transform to hand-drawn animation in the spirit of Studio Ghibli: soft lighting, gentle brush strokes, film-like texture. Retain the original color palette while applying the painterly look. Keep the original face, expression and composition.",
    watercolor:
        "Transform to watercolor painting: fluid washes, soft edges, paper texture, subtle granulation, light bloom. Preserve the original color palette and overall luminance; avoid strong hue shifts. Keep the original face, pose and composition.",
    "oil-painting":
        "Transform to classical oil painting on canvas: visible impasto brushwork, rich color depth, soft edges, realistic lighting. Preserve the original color palette (especially skin tones and key garments) with minimal deviation. Keep the original face, pose and composition.",
    sketches:
        "Transform to a colored pencil sketch: graphite-like hatching with clean linework and subtle shading on paper texture. Preserve the original color palette instead of converting to grayscale. Keep the original face, pose and composition.",
    cartoon: "Make this a 90s cartoon style. Keep the original face, pose and composition.",

};

// Pre-resize để giảm chi phí/độ trễ
async function preScale(buffer) {
    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) return buffer;
    const maxSide = Math.max(meta.width, meta.height);
    if (maxSide <= PERF.image.maxSidePx) return buffer;

    const scale = PERF.image.maxSidePx / maxSide;
    const W = Math.round((meta.width || 0) * scale);
    const H = Math.round((meta.height || 0) * scale);

    return await sharp(buffer)
        .resize(W, H, { fit: "inside" })
        .jpeg({ quality: 92 })
        .toBuffer();
}

function buildPrompt(style, extra) {
    const base = STYLE_PRESETS[style] || "";
    return extra && extra.trim()
        ? `${base}\nAdditional details: ${extra.trim()}`
        : base;
}

// Hỗ trợ cả FileOutput (SDK mới) lẫn URL string
async function readReplicateOutputToBuffer(out, { signal }) {
    const arr = Array.isArray(out) ? out : [out];
    const first = arr[0];

    if (first && typeof first?.blob === "function") {
        const blob = await first.blob();
        const ab = await blob.arrayBuffer();
        return Buffer.from(ab);
    }

    const url =
        typeof first === "string" ? first : first?.url || first?.toString?.();
    if (!url) throw new Error("Không xác định được output từ Replicate");
    const resp = await fetch(url, { signal });
    const ab = await resp.arrayBuffer();
    return Buffer.from(ab);
}

export const styleService = {
    applyStyle: async ({ inputBuffer, inputMime, style, extra, requestId }) => {
        const scaled = await preScale(inputBuffer);
        const prompt = buildPrompt(style, extra);

        // Hạn chế đồng thời các job Replicate nặng
        return await withReplicateLimiter(async () => {
            const controller = new AbortController();
            const timeoutMs = PERF.timeouts.replicateMs || 180_000;
            const timer = setTimeout(() => controller.abort(), timeoutMs);

            try {
                const runOnce = async (signal) => {
                    const out = await replicate.run(MODEL, {
                        input: {
                            input_image: scaled, // buffer — SDK sẽ stream lên
                            prompt,
                            // Có thể thêm aspect_ratio nếu muốn ép tỷ lệ, ví dụ "1:1", "3:4", "16:9"
                            // aspect_ratio: "original",
                        },
                        signal,
                        wait: true,
                    });
                    return await readReplicateOutputToBuffer(out, { signal });
                };

                const outputBuffer = await withRetry(
                    () => runOnce(controller.signal),
                    {
                        retries: 2,
                        baseDelayMs: 800,
                        factor: 2,
                        onRetry: (e, i) => {
                            // bạn có thể log bằng pino tại đây nếu muốn
                            if (process.env.NODE_ENV !== "production") {
                                console.warn(
                                    `[styleService] retry #${i + 1}`,
                                    e?.message
                                );
                            }
                        },
                    }
                );

                const ext = inputMime?.includes("png") ? "png" : "jpg";
                const { key } = await uploadBufferToR2(outputBuffer, {
                    contentType: ext === "png" ? "image/png" : "image/jpeg",
                    ext,
                    prefix: `styles/${style}`,
                });

                return {
                    key,
                    meta: { style, bytes: outputBuffer.length, requestId },
                };
            } finally {
                clearTimeout(timer);
            }
        });
    },
};
