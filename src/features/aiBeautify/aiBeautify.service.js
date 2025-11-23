// src/features/aiBeautify/aiBeautify.service.js
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";

// cjwbw/real-esrgan – Real-ESRGAN for high-quality image super-resolution
const REAL_ESRGAN_MODEL =
    "cjwbw/real-esrgan:42fed1c4974146d4d2414e2be2c5277c7fcf05fcc3a73abf41610695738c1d7b";

/**
 * Read Replicate output to buffer (supports both FileOutput and URL string)
 */
async function readReplicateOutputToBuffer(out) {
    const arr = Array.isArray(out) ? out : [out];
    const first = arr[0];

    if (first && typeof first?.blob === "function") {
        const blob = await first.blob();
        const ab = await blob.arrayBuffer();
        return Buffer.from(ab);
    }

    const url =
        typeof first === "string" ? first : first?.url || first?.toString?.();
    if (!url) throw new Error("Cannot determine output from Replicate");
    const resp = await fetch(url);
    const ab = await resp.arrayBuffer();
    return Buffer.from(ab);
}

/**
 * Main AI Beautify service using cjwbw/real-esrgan
 */
export const aiBeautifyService = {
    beautify: async ({ inputBuffer, inputMime, requestId, scale = 2 }) => {
        try {
            const outputBuffer = await withReplicateLimiter(async () => {
                const runOnce = async () => {
                    const out = await replicate.run(REAL_ESRGAN_MODEL, {
                        input: {
                            image: inputBuffer,
                            scale,
                        },
                        wait: true,
                    });
                    return await readReplicateOutputToBuffer(out);
                };

                return await withRetry(() => runOnce(), {
                    retries: 2,
                    baseDelayMs: 800,
                    factor: 2,
                    onRetry: (e, i) => {
                        if (process.env.NODE_ENV !== "production") {
                            console.warn(
                                `[aiBeautify] retry #${i + 1}`,
                                e?.message
                            );
                        }
                    },
                });
            });

            // Upload final buffer to R2
            const ext = inputMime?.includes("png") ? "png" : "jpg";
            const { key } = await uploadBufferToR2(outputBuffer, {
                contentType: ext === "png" ? "image/png" : "image/jpeg",
                ext,
                prefix: "aiBeautify",
            });

            return {
                key,
                meta: {
                    model: "cjwbw/real-esrgan",
                    version: REAL_ESRGAN_MODEL.split(":")[1],
                    scale,
                    bytes: outputBuffer.length,
                    requestId,
                    pipeline: ["cjwbw/real-esrgan"],
                },
            };
        } catch (error) {
            console.error("AI Beautify pipeline failed:", error);
            throw new Error(
                `AI Beautify failed: ${error.message || "Unknown error"}`
            );
        }
    },
};
