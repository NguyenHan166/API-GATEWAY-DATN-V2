import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { prescaleImage, readReplicateOutput } from "../../utils/image.js";
import { resultCache } from "../../utils/cache.js";
import { metrics } from "../../utils/metrics.js";

// Real-ESRGAN Model
const MODEL = "nightmareai/real-esrgan";

export const clarityService = {
    improveClarity: async ({
        inputBuffer,
        inputMime,
        scale,
        faceEnhance,
        requestId,
    }) => {
        const startTime = Date.now();
        try {
            // Check cache first
            const cacheKey = resultCache.makeKey(
                inputBuffer,
                {
                    scale,
                    faceEnhance,
                    feature: "improveClarity",
                },
                requestId
            );
            const cached = resultCache.get(cacheKey);
            if (cached) {
                const duration = Date.now() - startTime;
                metrics.recordRequest("improveClarity", duration, true, false);
                console.log(`[improveClarity] Cache hit: ${cacheKey}`);
                return cached;
            }

            // Hạn chế đồng thời các job Replicate nặng
            return await withReplicateLimiter(async () => {
                const {
                    buffer: scaled,
                    prescaled,
                    originalSize,
                    prescaledSize,
                } = await prescaleImage(inputBuffer, {
                    maxPixels: 2_000_000, // ~2MP
                    format: "jpeg",
                });

                const runOnce = async () => {
                    const out = await replicate.run(MODEL, {
                        input: {
                            image: scaled,
                            scale: scale, // 2 or 4
                            face_enhance: faceEnhance,
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
                        if (process.env.NODE_ENV !== "production") {
                            console.warn(
                                `[improveClarity] retry #${i + 1}`,
                                e?.message
                            );
                        }
                    },
                });

                // Upload to R2
                const ext = inputMime?.includes("png") ? "png" : "jpg";
                const { key } = await uploadBufferToR2(outputBuffer, {
                    contentType: ext === "png" ? "image/png" : "image/jpeg",
                    ext,
                    prefix: "clarity",
                });

                const meta = {
                    model: "real-esrgan",
                    scale,
                    faceEnhance,
                    bytes: outputBuffer.length,
                    requestId,
                };

                // Add prescale info if image was resized
                if (prescaled) {
                    meta.prescaled = true;
                    meta.originalSize = originalSize;
                    meta.prescaledSize = prescaledSize;
                }

                const result = { key, meta };

                // Cache result
                resultCache.set(cacheKey, result);

                // Record metrics
                const duration = Date.now() - startTime;
                metrics.recordRequest("improveClarity", duration, false, false);

                return result;
            }, "light"); // Light model - fast
        } catch (error) {
            const duration = Date.now() - startTime;
            metrics.recordRequest("improveClarity", duration, false, true);
            throw error;
        }
    },
};
