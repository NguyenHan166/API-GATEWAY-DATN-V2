// src/features/aiBeautify/aiBeautify.service.js
import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";

// cjwbw/real-esrgan – Real-ESRGAN for high-quality image super-resolution
const REAL_ESRGAN_MODEL =
    "cjwbw/real-esrgan:d0ee3d708c9b911f122a4ad90046c5d26a0293b99476d697f6bb7f2e251ce2d4";

// Max pixels supported by GPU (based on error: max 2096704 pixels)
// Use conservative limit to be safe
const MAX_INPUT_PIXELS = 2000000; // ~2MP (e.g., 1414x1414 or 2000x1000)

/**
 * Prescale image if it exceeds GPU memory limits
 * Maintains aspect ratio while reducing total pixel count
 */
async function prescaleIfNeeded(inputBuffer) {
    const metadata = await sharp(inputBuffer).metadata();
    const { width, height } = metadata;
    const totalPixels = width * height;

    if (totalPixels <= MAX_INPUT_PIXELS) {
        return {
            buffer: inputBuffer,
            prescaled: false,
            originalSize: { width, height },
        };
    }

    // Calculate new dimensions maintaining aspect ratio
    const scaleFactor = Math.sqrt(MAX_INPUT_PIXELS / totalPixels);
    const newWidth = Math.floor(width * scaleFactor);
    const newHeight = Math.floor(height * scaleFactor);

    console.log(
        `[aiBeautify] Prescaling image from ${width}x${height} (${totalPixels} pixels) ` +
            `to ${newWidth}x${newHeight} (${newWidth * newHeight} pixels)`
    );

    const resizedBuffer = await sharp(inputBuffer)
        .resize(newWidth, newHeight, { fit: "inside" })
        .toBuffer();

    return {
        buffer: resizedBuffer,
        prescaled: true,
        originalSize: { width, height },
        prescaledSize: { width: newWidth, height: newHeight },
    };
}

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
            // Prescale image if needed to fit GPU memory
            const {
                buffer: processBuffer,
                prescaled,
                originalSize,
                prescaledSize,
            } = await prescaleIfNeeded(inputBuffer);

            const outputBuffer = await withReplicateLimiter(async () => {
                const runOnce = async () => {
                    const out = await replicate.run(REAL_ESRGAN_MODEL, {
                        input: {
                            image: processBuffer,
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
                                `[aiBeautify] retry#${i + 1}`,
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

            return { key, meta };
        } catch (error) {
            console.error("AI Beautify pipeline failed:", error);
            throw new Error(
                `AI Beautify failed: ${error.message || "Unknown error"}`
            );
        }
    },
};
