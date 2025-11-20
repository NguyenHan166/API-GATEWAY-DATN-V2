import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { uploadBufferToR2 } from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";

// Real-ESRGAN Image Upscale Model
const MODEL = "nightmareai/real-esrgan";
const MAX_SIDE = 2560;
const MAX_PIXELS = 2_000_000; // guard GPU memory limit (~2.096MP)

// Pre-resize để tránh ảnh quá lớn và vượt giới hạn GPU của Real-ESRGAN
async function preScale(buffer, maxSide = MAX_SIDE, maxPixels = MAX_PIXELS) {
    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) return buffer;

    const { width, height } = meta;
    const currentMaxSide = Math.max(width, height);
    const currentPixels = width * height;

    // Tính scale theo cả chiều dài max và tổng pixel, lấy giá trị nhỏ nhất
    const scaleBySide = maxSide ? maxSide / currentMaxSide : 1;
    const scaleByPixels = maxPixels
        ? Math.sqrt(maxPixels / currentPixels)
        : 1;
    const scale = Math.min(1, scaleBySide, scaleByPixels);
    if (!isFinite(scale) || scale >= 1) return buffer;

    const W = Math.max(1, Math.round(width * scale));
    const H = Math.max(1, Math.round(height * scale));

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
        faceEnhance,
        requestId,
    }) => {
        // Hạn chế đồng thời các job Replicate nặng
        return await withReplicateLimiter(async () => {
            const scaled = await preScale(inputBuffer);

            const runOnce = async () => {
                const out = await replicate.run(MODEL, {
                    input: {
                        image: scaled,
                        scale, // 2 hoặc 4
                        face_enhance: faceEnhance,
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
                prefix: "enhance/real-esrgan",
            });

            return {
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
        });
    },
};
