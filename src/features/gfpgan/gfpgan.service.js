// src/features/gfpgan/gfpgan.service.js
import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import {
    uploadBufferToR2,
    presignGetUrl,
} from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { PERF } from "../../config/perf.js";

const MODEL =
    "tencentarc/gfpgan:297a243ce8643961d52f745f9b6c8c1bd96850a51c92be5f43628a0d3e08321a";

// Giữ cách tiền xử lý/đọc output tương tự replaceStyle.service
async function preScale(buffer) {
    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) return buffer;
    const maxSide = Math.max(meta.width, meta.height);
    if (maxSide <= PERF.image.maxSidePx) return buffer;

    const scale = PERF.image.maxSidePx / maxSide;
    const W = Math.round((meta.width || 0) * scale);
    const H = Math.round((meta.height || 0) * scale);

    return await sharp(buffer)
        .resize(W, H, { fit: "inside" })
        .jpeg({ quality: 92 })
        .toBuffer();
}

// Hỗ trợ cả FileOutput (SDK mới) lẫn URL string (y như replaceStyle)
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

export const gfpganService = {
    enhance: async ({ inputBuffer, inputMime, scale, version, requestId }) => {
        // 1) Pre-scale để tránh output quá lớn
        const scaled = await preScale(inputBuffer);

        // 2) Upload input -> R2 -> presign GET (Replicate cần URL)
        const inputExt = inputMime?.includes("png") ? "png" : "jpg";
        const { key: inputKey } = await uploadBufferToR2(scaled, {
            contentType: inputExt === "png" ? "image/png" : "image/jpeg",
            ext: inputExt,
            prefix: `gfpgan/input`,
        });
        const inputSignedUrl = await presignGetUrl(inputKey, 15 * 60); // 15 phút đủ cho prediction

        return await withReplicateLimiter(async () => {
            const runOnce = async () => {
                // Dùng Predictions API để có id + logs khi fail upload output
                const prediction = await replicate.predictions.create({
                    version: MODEL,
                    input: { img: inputSignedUrl, scale, version }, // dùng presigned URL
                    wait: true,
                });
                if (prediction.status !== "succeeded") {
                    const err = new Error(
                        `Prediction ${prediction.id} failed: ${
                            prediction.error || "unknown"
                        }`
                    );
                    err.predictionId = prediction.id;
                    err.predictionLogs = prediction.logs;
                    throw err;
                }
                return await readReplicateOutputToBuffer(prediction.output);
            };

            const outputBuffer = await withRetry(
                async () => {
                    try {
                        return await runOnce();
                    } catch (e) {
                        const msg = String(e?.message || "");
                        if (/upload output files/i.test(msg)) {
                            await new Promise((r) => setTimeout(r, 1200));
                            return await runOnce();
                        }
                        throw e;
                    }
                },
                { retries: 2, baseDelayMs: 800, factor: 2 }
            );

            const ext = "jpg"; // output: ưu tiên JPEG cho nhẹ, đủ chất lượng
            const { key } = await uploadBufferToR2(outputBuffer, {
                contentType: ext === "png" ? "image/png" : "image/jpeg",
                ext,
                prefix: `gfpgan/s${scale}-${version}`,
            });

            return {
                key,
                meta: { scale, version, bytes: outputBuffer.length, requestId },
            };
        });
    },
};
