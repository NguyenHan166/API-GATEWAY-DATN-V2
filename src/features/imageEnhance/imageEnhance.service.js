import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";

// Topaz Labs Image Upscale Model
const MODEL = "topazlabs/image-upscale";

// Pre-resize để tránh ảnh quá lớn - Topaz Labs có thể xử lý ảnh lớn hơn
async function preScale(buffer, maxSide = 4096) {
    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) return buffer;
    const currentMaxSide = Math.max(meta.width, meta.height);
    if (currentMaxSide <= maxSide) return buffer;

    const scale = maxSide / currentMaxSide;
    const W = Math.round((meta.width || 0) * scale);
    const H = Math.round((meta.height || 0) * scale);

    return await sharp(buffer)
        .resize(W, H, { fit: "inside" })
        .jpeg({ quality: 92 })
        .toBuffer();
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

export const enhanceService = {
    enhanceImage: async ({
        inputBuffer,
        inputMime,
        scale,
        model,
        requestId,
    }) => {
        // Hạn chế đồng thời các job Replicate nặng
        return await withReplicateLimiter(async () => {
            const scaled = await preScale(inputBuffer);

            const runOnce = async () => {
                const out = await replicate.run(MODEL, {
                    input: {
                        image: scaled,
                        scale: scale, // 2, 4, or 6
                        model: model, // "standard-v2", "high-fidelity-v2", etc.
                    },
                    wait: true,
                });
                return await readReplicateOutputToBuffer(out);
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
                prefix: `enhance/${model}`,
            });

            return {
                key,
                meta: {
                    provider: "topaz-labs",
                    model,
                    scale,
                    bytes: outputBuffer.length,
                    requestId,
                },
            };
        });
    },
};
