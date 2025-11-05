import sharp from "sharp";
import { removeBackgroundToPngRGBA } from "../../integrations/replicate/background.service.js";
import {
    fitBackground,
    featherAlpha,
    makeShadowFromAlpha,
} from "../../utils/image.js";

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
