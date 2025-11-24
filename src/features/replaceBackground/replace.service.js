import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import {
    fitBackground,
    featherAlpha,
    makeShadowFromAlpha,
    prescaleImage,
} from "../../utils/image.js";

// PIN phiên bản model giống phía Python để tránh regression
const MODEL =
    "851-labs/background-remover:a029dff38972b5fda4ec5d75d7d1cd25aeff621d2cf4946a41055d7db66b80bc";

async function removeBackgroundToPngRGBA(inputBuffer) {
    const { buffer: scaled } = await prescaleImage(inputBuffer, {
        format: "jpeg",
    });

    const runOnce = async () => {
        const out = await replicate.run(MODEL, {
            input: {
                image: scaled,
                background_type: "rgba",
                format: "png",
            },
        });
        const url = Array.isArray(out) ? out[0] : out;
        const buf = await fetch(url)
            .then((r) => r.arrayBuffer())
            .then((b) => Buffer.from(b));
        return buf;
    };

    return await withRetry(() => runOnce(), {
        retries: 2,
        baseDelayMs: 800,
        factor: 2,
    });
}

export async function removeBackground({ fgBuf, featherPx = 1 }) {
    const cutout = await removeBackgroundToPngRGBA(fgBuf);
    const { width: W, height: H } = await sharp(fgBuf).metadata();

    // Chỉ cần feather alpha nếu cần
    const finalPng =
        featherPx > 0 ? await featherAlpha(cutout, featherPx) : cutout;

    return { finalPng, W, H };
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

export const replaceBgService = { removeBackground, replaceBackground };
