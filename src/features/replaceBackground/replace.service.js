import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { PERF } from "../../config/perf.js";
import { withRetry } from "../../utils/retry.js";
import {
    fitBackground,
    featherAlpha,
    makeShadowFromAlpha,
} from "../../utils/image.js";

// PIN phiên bản model giống phía Python để tránh regression
const MODEL =
    "851-labs/background-remover:a029dff38972b5fda4ec5d75d7d1cd25aeff621d2cf4946a41055d7db66b80bc";

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

async function removeBackgroundToPngRGBA(inputBuffer) {
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
    const timeoutMs = PERF.timeouts.replicateMs;
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    try {
        return await withRetry(() => runOnce(controller.signal), {
            retries: 2,
            baseDelayMs: 800,
            factor: 2,
        });
    } finally {
        clearTimeout(timer);
    }
}

export async function replaceBackground({
    fgBuf,
    bgBuf,
    fit = "cover",
    position = "centre",
    featherPx = 1,
    addShadow = true,
}) {
    const cutout = await removeBackgroundToPngRGBA(fgBuf);
    const { width: W, height: H } = await sharp(fgBuf).metadata();
    const bgResized = await fitBackground(bgBuf, W, H, fit, position);
    const cutoutFeathered = await featherAlpha(cutout, featherPx);

    let composed = sharp(bgResized);
    if (addShadow) {
        const { blackLayer, shadowMask } = await makeShadowFromAlpha(
            cutoutFeathered,
            W,
            H,
            0.35,
            8
        );
        composed = composed.composite([
            { input: blackLayer, blend: "over", mask: shadowMask },
        ]);
    }

    const finalPng = await composed
        .composite([{ input: cutoutFeathered, blend: "over" }])
        .png()
        .toBuffer();

    return { finalPng, W, H };
}

export const replaceBgService = { replaceBackground };
