import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { PERF } from "../../config/perf.js";

// Real-ESRGAN Model
const MODEL = "nightmareai/real-esrgan";

// Max pixels supported by GPU (based on error: max 2096704 pixels)
// Use conservative limit to be safe
const MAX_INPUT_PIXELS = 2000000; // ~2MP (e.g., 1414x1414 or 2000x1000)

/**
 * Pre-resize để tránh ảnh quá lớn vượt GPU memory limit
 * Giới hạn theo tổng số pixels thay vì chiều dài cạnh
 */
async function preScale(buffer) {
    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) return { buffer, prescaled: false };

    const totalPixels = meta.width * meta.height;

    if (totalPixels <= MAX_INPUT_PIXELS) {
        return {
            buffer,
            prescaled: false,
            originalSize: { width: meta.width, height: meta.height },
        };
    }

    // Calculate new dimensions maintaining aspect ratio
    const scaleFactor = Math.sqrt(MAX_INPUT_PIXELS / totalPixels);
    const newWidth = Math.floor(meta.width * scaleFactor);
    const newHeight = Math.floor(meta.height * scaleFactor);

    console.log(
        `[improveClarity] Prescaling image from ${meta.width}x${meta.height} (${totalPixels} pixels) ` +
            `to ${newWidth}x${newHeight} (${newWidth * newHeight} pixels)`
    );

    const resizedBuffer = await sharp(buffer)
        .resize(newWidth, newHeight, { fit: "inside" })
        .jpeg({ quality: 92 })
        .toBuffer();

    return {
        buffer: resizedBuffer,
        prescaled: true,
        originalSize: { width: meta.width, height: meta.height },
        prescaledSize: { width: newWidth, height: newHeight },
    };
}

// Hỗ trợ cả FileOutput (SDK mới) lẫn URL string
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
    if (!url) throw new Error("Không xác định được output từ Replicate");
    const resp = await fetch(url);
    const ab = await resp.arrayBuffer();
    return Buffer.from(ab);
}

export const clarityService = {
    improveClarity: async ({
        inputBuffer,
        inputMime,
        scale,
        faceEnhance,
        requestId,
    }) => {
        // Hạn chế đồng thời các job Replicate nặng
        return await withReplicateLimiter(async () => {
            const {
                buffer: scaled,
                prescaled,
                originalSize,
                prescaledSize,
            } = await preScale(inputBuffer);

            const runOnce = async () => {
                const out = await replicate.run(MODEL, {
                    input: {
                        image: scaled,
                        scale: scale, // 2 or 4
                        face_enhance: faceEnhance,
                    },
                    wait: true,
                });
                return await readReplicateOutputToBuffer(out);
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

            return { key, meta };
        });
    },
};
