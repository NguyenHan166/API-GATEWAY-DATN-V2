import sharp from "sharp";

export async function fitBackground(
    bgBuffer,
    W,
    H,
    fit = "cover",
    position = "centre"
) {
    return await sharp(bgBuffer)
        .resize({ width: W, height: H, fit, position })
        .toBuffer();
}

export async function featherAlpha(pngWithAlphaBuffer, px = 1) {
    if (!px || px <= 0) return pngWithAlphaBuffer;
    const alpha = await sharp(pngWithAlphaBuffer)
        .ensureAlpha()
        .extractChannel("alpha")
        .blur(px)
        .toBuffer();
    return await sharp(pngWithAlphaBuffer).joinChannel(alpha).png().toBuffer();
}

export async function makeShadowFromAlpha(
    pngWithAlphaBuffer,
    W,
    H,
    opacity = 0.35,
    blurPx = 8
) {
    const alpha = await sharp(pngWithAlphaBuffer)
        .extractChannel("alpha")
        .toBuffer();
    const shadowMask = await sharp(alpha).blur(blurPx).png().toBuffer();
    const blackLayer = await sharp({
        create: {
            width: W,
            height: H,
            channels: 4,
            background: { r: 0, g: 0, b: 0, alpha: opacity },
        },
    })
        .png()
        .toBuffer();
    return { blackLayer, shadowMask };
}
