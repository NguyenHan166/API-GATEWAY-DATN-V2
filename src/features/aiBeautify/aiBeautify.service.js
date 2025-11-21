// src/features/aiBeautify/aiBeautify.service.js
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";

// alexgenovese/upscaler (GFPGAN-based) – pinned version for stability
const UPSCALER_MODEL =
    "alexgenovese/upscaler:4f7eb3da655b5182e559d50a0437440f242992d47e5e20bd82829a79dee61ff3";

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
 * Main AI Beautify service using alexgenovese/upscaler
 */
export const aiBeautifyService = {
    beautify: async ({
        inputBuffer,
        inputMime,
        requestId,
        scale = 4,
        faceEnhance = true,
    }) => {
        try {
            const outputBuffer = await withReplicateLimiter(async () => {
                const runOnce = async () => {
                    const out = await replicate.run(UPSCALER_MODEL, {
                        input: {
                            image: inputBuffer,
                            scale,
                            face_enhance: faceEnhance,
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
                    model: "alexgenovese/upscaler",
                    version: UPSCALER_MODEL.split(":")[1],
                    scale,
                    faceEnhance,
                    bytes: outputBuffer.length,
                    requestId,
                    pipeline: [
                        "alexgenovese/upscaler",
                    ],
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
