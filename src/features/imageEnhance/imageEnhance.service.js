import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { prescaleImage, readReplicateOutput } from "../../utils/image.js";
import { resultCache } from "../../utils/cache.js";
import { metrics } from "../../utils/metrics.js";

// Real-ESRGAN Image Upscale Model
const MODEL = "nightmareai/real-esrgan";

export const enhanceService = {
    enhanceImage: async ({
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
                    feature: "imageEnhance",
                },
                requestId
            );
            const cached = resultCache.get(cacheKey);
            if (cached) {
                const duration = Date.now() - startTime;
                metrics.recordRequest("imageEnhance", duration, true, false);
                console.log(`[imageEnhance] Cache hit: ${cacheKey}`);
                return cached;
            }

            // Hạn chế đồng thời các job Replicate nặng
            return await withReplicateLimiter(async () => {
                const { buffer: scaled } = await prescaleImage(inputBuffer, {
                    maxPixels: 2_000_000, // ~2MP
                    maxSide: 2560,
                    format: "jpeg",
                });

                const runOnce = async () => {
                    const out = await replicate.run(MODEL, {
                        input: {
                            image: scaled,
                            scale, // 2 hoặc 4
                            face_enhance: faceEnhance,
                        },
                        wait: true,
                    });
                    return await readReplicateOutput(out);
                };

                const outputBuffer = await withRetry(() => runOnce(), {
                    retries: 2,
                    baseDelayMs: 1000,
                    factor: 2,
                    onRetry: (e, i) => {
                        if (process.env.NODE_ENV !== "production") {
                            console.warn(
                                `[imageEnhance] retry #${i + 1}`,
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
                    prefix: "enhance/real-esrgan",
                });

                const result = {
                    key,
                    meta: {
                        provider: "nightmareai",
                        model: "real-esrgan",
                        scale,
                        faceEnhance,
                        bytes: outputBuffer.length,
                        requestId,
                    },
                };

                // Cache result
                resultCache.set(cacheKey, result);

                // Record metrics
                const duration = Date.now() - startTime;
                metrics.recordRequest("imageEnhance", duration, false, false);

                return result;
            }, "light"); // Light model - fast
        } catch (error) {
            const duration = Date.now() - startTime;
            metrics.recordRequest("imageEnhance", duration, false, true);
            throw error;
        }
    },
};
