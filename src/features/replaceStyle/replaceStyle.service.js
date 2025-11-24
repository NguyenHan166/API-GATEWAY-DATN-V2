import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { prescaleImage, readReplicateOutput } from "../../utils/image.js";
import { resultCache } from "../../utils/cache.js";
import { metrics } from "../../utils/metrics.js";

// Model FLUX Kontext Pro — State-of-the-art text-based image editing
const MODEL = "black-forest-labs/flux-kontext-pro";

// Preset prompt cho từng style - Optimized for flux-kontext-pro with specific, detailed language
const STYLE_PRESETS = {
    anime: "Change the image to anime cel-shaded art style with clean black ink outlines and flat 2-3 tone shading. Use vibrant colors with slight saturation boost. Keep the exact same facial features, expression, pose, and composition. Add specular highlights to the eyes. Maintain the original lighting direction and background elements in anime style.",
    ghibli: "Change the image to hand-drawn Studio Ghibli animation style with soft natural lighting, gentle watercolor-like brush strokes, and subtle film grain texture. Keep the exact same facial features, expression, pose, and composition. Preserve warm, earthy tones with painterly quality. Maintain the original background elements in Ghibli style.",
    watercolor:
        "Change the image to watercolor painting style with fluid transparent washes, soft blended edges, visible paper texture, subtle color granulation, and light bloom effects. Keep the exact same facial features, expression, pose, and composition. Preserve the original color palette and luminance values. Maintain natural skin tones.",
    "oil-painting":
        "Change the image to classical oil painting on canvas with visible thick impasto brushwork, rich color depth, soft blended edges, and realistic chiaroscuro lighting. Keep the exact same facial features, expression, pose, and composition. Preserve the original color palette especially for skin tones and key garments. Use traditional Renaissance techniques.",
    sketches:
        "Change the image to colored pencil sketch style with fine graphite-like hatching, clean precise linework, subtle cross-hatch shading on textured paper. Keep the exact same facial features, expression, pose, and composition. Preserve the original color palette with pencil-drawn appearance instead of converting to grayscale.",
    cartoon:
        "Change the image to 1990s animated cartoon style with bold outlines, simplified features, and vibrant flat colors. Keep the exact same facial features, expression, pose, and composition. Use classic hand-drawn animation techniques with cel-shading.",
};

function buildPrompt(style, extra) {
    const base = STYLE_PRESETS[style] || "";
    return extra && extra.trim()
        ? `${base}\nAdditional details: ${extra.trim()}`
        : base;
}

export const styleService = {
    applyStyle: async ({ inputBuffer, inputMime, style, extra, requestId }) => {
        const startTime = Date.now();
        try {
            // Check cache first
            const cacheKey = resultCache.makeKey(
                inputBuffer,
                {
                    style,
                    extra,
                    feature: "replaceStyle",
                },
                requestId
            );
            const cached = resultCache.get(cacheKey);
            if (cached) {
                const duration = Date.now() - startTime;
                metrics.recordRequest("replaceStyle", duration, true, false);
                console.log(`[replaceStyle] Cache hit: ${cacheKey}`);
                return cached;
            }

            const { buffer: scaled } = await prescaleImage(inputBuffer, {
                format: "jpeg",
            });
            const prompt = buildPrompt(style, extra);

            // Hạn chế đồng thời các job Replicate nặng
            return await withReplicateLimiter(async () => {
                const runOnce = async () => {
                    const out = await replicate.run(MODEL, {
                        input: {
                            input_image: scaled, // buffer — SDK sẽ stream lên
                            prompt,
                            // Có thể thêm aspect_ratio nếu muốn ép tỷ lệ, ví dụ "1:1", "3:4", "16:9"
                            // aspect_ratio: "original",
                        },
                        wait: true,
                    });
                    return await readReplicateOutput(out);
                };

                const outputBuffer = await withRetry(() => runOnce(), {
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
                });

                const ext = inputMime?.includes("png") ? "png" : "jpg";
                const { key } = await uploadBufferToR2(outputBuffer, {
                    contentType: ext === "png" ? "image/png" : "image/jpeg",
                    ext,
                    prefix: `styles/${style}`,
                });

                const result = {
                    key,
                    meta: { style, bytes: outputBuffer.length, requestId },
                };

                // Cache result
                resultCache.set(cacheKey, result);

                // Record metrics
                const duration = Date.now() - startTime;
                metrics.recordRequest("replaceStyle", duration, false, false);

                return result;
            }, "heavy"); // Heavy model - slow FLUX
        } catch (error) {
            const duration = Date.now() - startTime;
            metrics.recordRequest("replaceStyle", duration, false, true);
            throw error;
        }
    },
};
