import sharp from "sharp";
import { PERF } from "../config/perf.js";

/**
 * Unified pre-scaling for all features
 * Handles both pixel count limits and max side length limits
 *
 * @param {Buffer} buffer - Input image buffer
 * @param {Object} options - Scaling options
 * @param {number} options.maxPixels - Maximum total pixels (default: 2MP)
 * @param {number} options.maxSide - Maximum side length in pixels (default: from PERF)
 * @param {number} options.quality - JPEG quality (default: 92)
 * @param {string} options.format - Output format: 'jpeg' | 'png' | 'webp' (default: 'jpeg')
 * @returns {Promise<{buffer: Buffer, prescaled: boolean, originalSize: Object, prescaledSize?: Object}>}
 */
export async function prescaleImage(buffer, options = {}) {
    const {
        maxPixels = 2_000_000, // 2MP default (~1414x1414)
        maxSide = PERF.image.maxSidePx,
        quality = PERF.image.outputQuality,
        format = "jpeg",
    } = options;

    const meta = await sharp(buffer).metadata();
    if (!meta.width || !meta.height) {
        return {
            buffer,
            prescaled: false,
            originalSize: { width: 0, height: 0 },
        };
    }

    const { width, height } = meta;
    const totalPixels = width * height;
    const currentMaxSide = Math.max(width, height);

    // Check both pixel count and side length
    if (totalPixels <= maxPixels && currentMaxSide <= maxSide) {
        return {
            buffer,
            prescaled: false,
            originalSize: { width, height },
        };
    }

    // Calculate scale factor (most restrictive wins)
    const pixelScaleFactor = Math.sqrt(maxPixels / totalPixels);
    const sideScaleFactor = maxSide / currentMaxSide;
    const scaleFactor = Math.min(pixelScaleFactor, sideScaleFactor);

    const newWidth = Math.floor(width * scaleFactor);
    const newHeight = Math.floor(height * scaleFactor);

    console.log(
        `[prescaleImage] ${width}x${height} (${totalPixels}px) → ${newWidth}x${newHeight} (${
            newWidth * newHeight
        }px)`
    );

    const resizedBuffer = await sharp(buffer)
        .resize(newWidth, newHeight, { fit: "inside" })
        [format]({ quality })
        .toBuffer();

    return {
        buffer: resizedBuffer,
        prescaled: true,
        originalSize: { width, height },
        prescaledSize: { width: newWidth, height: newHeight },
    };
}

/**
 * Unified Replicate output reader
 * Supports both FileOutput (SDK v1.0+) and URL strings
 *
 * @param {any} output - Replicate output (FileOutput or URL string)
 * @returns {Promise<Buffer>} - Image buffer
 */
export async function readReplicateOutput(output) {
    const arr = Array.isArray(output) ? output : [output];
    const first = arr[0];

    // FileOutput (new SDK)
    if (first && typeof first?.blob === "function") {
        const blob = await first.blob();
        const ab = await blob.arrayBuffer();
        return Buffer.from(ab);
    }

    // URL string (legacy)
    const url =
        typeof first === "string" ? first : first?.url || first?.toString?.();
    if (!url) throw new Error("Invalid Replicate output format");

    const resp = await fetch(url);
    if (!resp.ok) throw new Error(`Fetch failed: ${resp.status}`);

    const ab = await resp.arrayBuffer();
    return Buffer.from(ab);
}

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
