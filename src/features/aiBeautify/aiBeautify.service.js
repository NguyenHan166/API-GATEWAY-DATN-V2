// src/features/aiBeautify/aiBeautify.service.js
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { prescaleImage, readReplicateOutput } from "../../utils/image.js";
import { resultCache } from "../../utils/cache.js";
import { metrics } from "../../utils/metrics.js";

// cjwbw/real-esrgan – Real-ESRGAN for high-quality image super-resolution
const REAL_ESRGAN_MODEL =
    "cjwbw/real-esrgan:d0ee3d708c9b911f122a4ad90046c5d26a0293b99476d697f6bb7f2e251ce2d4";

/**
 * Main AI Beautify service using cjwbw/real-esrgan
 */
export const aiBeautifyService = {
    beautify: async ({ inputBuffer, inputMime, requestId, scale = 2 }) => {
        const startTime = Date.now();
        try {
            // Check cache first
            const cacheKey = resultCache.makeKey(
                inputBuffer,
                {
                    scale,
                    feature: "aiBeautify",
                },
                requestId
            );
            const cached = resultCache.get(cacheKey);
            if (cached) {
                const duration = Date.now() - startTime;
                metrics.recordRequest("aiBeautify", duration, true, false);
                console.log(`[aiBeautify] Cache hit: ${cacheKey}`);
                return cached;
            }
            // Prescale image if needed to fit GPU memory
            const {
                buffer: processBuffer,
                prescaled,
                originalSize,
                prescaledSize,
            } = await prescaleImage(inputBuffer, {
                maxPixels: 2_000_000, // ~2MP
                format: "jpeg",
            });

            const outputBuffer = await withReplicateLimiter(async () => {
                const runOnce = async () => {
                    const out = await replicate.run(REAL_ESRGAN_MODEL, {
                        input: {
                            image: processBuffer,
                            scale,
                        },
                        wait: true,
                    });
                    return await readReplicateOutput(out);
                };

                return await withRetry(() => runOnce(), {
                    retries: 2,
                    baseDelayMs: 800,
                    factor: 2,
                    onRetry: (e, i) => {
                        if (process.env.NODE_ENV !== "production") {
                            console.warn(
                                `[aiBeautify] retry#${i + 1}`,
                                e?.message
                            );
                        }
                    },
                });
            }, "light"); // Light model - fast

            // Upload final buffer to R2
            const ext = inputMime?.includes("png") ? "png" : "jpg";
            const { key } = await uploadBufferToR2(outputBuffer, {
                contentType: ext === "png" ? "image/png" : "image/jpeg",
                ext,
                prefix: "aiBeautify",
            });

            const meta = {
                model: "cjwbw/real-esrgan",
                version: REAL_ESRGAN_MODEL.split(":")[1],
                scale,
                bytes: outputBuffer.length,
                requestId,
                pipeline: ["cjwbw/real-esrgan"],
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
            metrics.recordRequest("aiBeautify", duration, false, false);

            return result;
        } catch (error) {
            const duration = Date.now() - startTime;
            metrics.recordRequest("aiBeautify", duration, false, true);
            console.error("AI Beautify pipeline failed:", error);
            throw new Error(
                `AI Beautify failed: ${error.message || "Unknown error"}`
            );
        }
    },
};
