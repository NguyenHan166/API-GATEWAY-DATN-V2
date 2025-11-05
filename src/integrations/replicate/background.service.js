import { replicate } from "./client.js";
import sharp from "sharp";
import { PERF } from "../../config/perf.js";
import { withRetry } from "../../utils/retry.js";

// PIN phiên bản model (sửa nếu bạn dùng version khác)
const MODEL =
    "851-labs/background-remover:a029dff38972b5fda4ec5d75d7d1cd25aeff621d2cf4946a41055d7db66b80bc";

// Resize ảnh về long-edge ≤ PERF.image.maxSidePx để tiết kiệm chi phí
async function preScale(buffer) {
    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) return buffer;
    const { width, height } = meta;
    const maxSide = Math.max(width, height);
    if (maxSide <= PERF.image.maxSidePx) return buffer;

    const scale = PERF.image.maxSidePx / maxSide;
    const W = Math.round(width * scale);
    const H = Math.round(height * scale);
    return await sharp(buffer)
        .resize(W, H, { fit: "inside" })
        .jpeg({ quality: 92 })
        .toBuffer();
}

export async function removeBackgroundToPngRGBA(inputBuffer) {
    const scaled = await preScale(inputBuffer);

    const runOnce = async (signal) => {
        const out = await replicate.run(MODEL, {
            input: {
                image: scaled,
                background_type: "rgba",
                format: "png",
            },
            signal,
        });
        const url = Array.isArray(out) ? out[0] : out;
        const buf = await fetch(url, { signal })
            .then((r) => r.arrayBuffer())
            .then((b) => Buffer.from(b));
        return buf;
    };

    const controller = new AbortController();
    const t = setTimeout(() => controller.abort(), PERF.timeouts.replicateMs);

    try {
        return await withRetry(() => runOnce(controller.signal), {
            retries: 2,
            baseDelayMs: 800,
            factor: 2,
        });
    } finally {
        clearTimeout(t);
    }
}
