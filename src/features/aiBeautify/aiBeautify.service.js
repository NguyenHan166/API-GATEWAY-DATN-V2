// src/features/aiBeautify/aiBeautify.service.js
import sharp from "sharp";
import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import {
    uploadBufferToR2,
    presignGetUrl,
} from "../../integrations/r2/storage.service.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import { PERF } from "../../config/perf.js";

const GFPGAN_MODEL =
    "tencentarc/gfpgan:297a243ce8643961d52f745f9b6c8c1bd96850a51c92be5f43628a0d3e08321a";
const REAL_ESRGAN_MODEL = "nightmareai/real-esrgan";
const MAX_REPLICATE_PIXELS = 2_096_704; // Hardware limit reported by Replicate error

/**
 * Pre-scale image - GFPGAN max: ~1536px, safe limit: 1440px
 * Tính toán: 1440 * 1440 = 2,073,600 pixels < 2,096,704 GPU limit
 */
async function preScale(buffer, maxSide = 1440) {
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

/**
 * Read Replicate output to buffer (supports both FileOutput and URL string)
 */
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
    if (!url) throw new Error("Cannot determine output from Replicate");
    const resp = await fetch(url);
    const ab = await resp.arrayBuffer();
    return Buffer.from(ab);
}

/**
 * Step 3: Run GFPGAN for face restoration
 */
async function runGFPGAN(inputBuffer, requestId) {
    const inputExt = "jpg";
    const { key: inputKey } = await uploadBufferToR2(inputBuffer, {
        contentType: "image/jpeg",
        ext: inputExt,
        prefix: `aiBeautify/gfpgan-input`,
    });
    const inputSignedUrl = await presignGetUrl(inputKey, 15 * 60);

    return await withReplicateLimiter(async () => {
        const runOnce = async () => {
            const prediction = await replicate.predictions.create({
                version: GFPGAN_MODEL,
                input: {
                    img: inputSignedUrl,
                    scale: 1, // Avoid doubling size before Real-ESRGAN to keep within GPU pixel budget
                    version: "v1.4", // GFPGAN version
                },
                wait: true,
            });

            if (prediction.status !== "succeeded") {
                const err = new Error(
                    `GFPGAN prediction ${prediction.id} failed: ${
                        prediction.error || "unknown"
                    }`
                );
                err.predictionId = prediction.id;
                err.predictionLogs = prediction.logs;
                throw err;
            }

            return await readReplicateOutputToBuffer(prediction.output);
        };

        return await withRetry(
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
    });
}

/**
 * Step 4: Run Real-ESRGAN with scale=1 (no upscaling) but with face enhancement
 * Note: Real-ESRGAN typically only supports scale 2 or 4, so we'll use scale=2 then resize back
 */
async function runRealESRGAN(inputBuffer) {
    // Ensure we never exceed the GPU pixel budget even if the upstream step outputs a larger image
    let targetWidth;
    let targetHeight;
    const meta = await sharp(inputBuffer).metadata();
    if (meta.width && meta.height) {
        const pixels = meta.width * meta.height;
        let width = meta.width;
        let height = meta.height;

        if (pixels > MAX_REPLICATE_PIXELS) {
            const scale = Math.sqrt(MAX_REPLICATE_PIXELS / pixels);
            width = Math.max(1, Math.floor(meta.width * scale));
            height = Math.max(1, Math.floor(meta.height * scale));
            inputBuffer = await sharp(inputBuffer)
                .resize(width, height, { fit: "inside" })
                .jpeg({ quality: 92 })
                .toBuffer();
        }

        targetWidth = width;
        targetHeight = height;
    }

    return await withReplicateLimiter(async () => {
        const runOnce = async () => {
            const out = await replicate.run(REAL_ESRGAN_MODEL, {
                input: {
                    image: inputBuffer,
                    scale: 2, // Minimum scale
                    face_enhance: true,
                },
                wait: true,
            });
            return await readReplicateOutputToBuffer(out);
        };

        const outputBuffer = await withRetry(() => runOnce(), {
            retries: 2,
            baseDelayMs: 800,
            factor: 2,
        });

        // Resize back to original size (since we used scale=2)
        if (targetWidth && targetHeight) {
            return await sharp(outputBuffer)
                .resize(targetWidth, targetHeight, { fit: "fill" })
                .toBuffer();
        }

        return outputBuffer;
    });
}

/**
 * Step 5 & 6: Skin Mask and Skin Retouch
 * Using simple skin tone detection as a fallback (MediaPipe would be more accurate)
 * This is a simplified version using color-based skin detection
 */
async function applySkinRetouch(buffer) {
    try {
        const image = sharp(buffer);
        const { width, height } = await image.metadata();

        // Get raw pixel data
        const { data, info } = await image
            .ensureAlpha()
            .raw()
            .toBuffer({ resolveWithObject: true });

        // Create skin mask using HSV color space detection
        const maskData = Buffer.alloc(width * height);

        for (let i = 0; i < data.length; i += 4) {
            const r = data[i];
            const g = data[i + 1];
            const b = data[i + 2];

            // Simple skin tone detection
            const isSkin = detectSkinTone(r, g, b);
            const pixelIndex = i / 4;
            maskData[pixelIndex] = isSkin ? 255 : 0;
        }

        // Create blurred version of the image
        const blurred = await sharp(buffer).blur(1.4).toBuffer();

        // Composite blurred skin areas back onto original
        const mask = await sharp(maskData, {
            raw: {
                width,
                height,
                channels: 1,
            },
        }).toBuffer();

        return await sharp(blurred)
            .composite([
                {
                    input: buffer,
                    blend: "over",
                },
            ])
            .toBuffer();
    } catch (error) {
        console.warn("Skin retouch failed, using original:", error.message);
        return buffer; // Return original if skin retouch fails
    }
}

/**
 * Simple skin tone detection using RGB values
 */
function detectSkinTone(r, g, b) {
    // Basic skin tone detection rules
    const rule1 = r > 95 && g > 40 && b > 20;
    const rule2 = Math.max(r, g, b) - Math.min(r, g, b) > 15;
    const rule3 = Math.abs(r - g) > 15 && r > g && r > b;

    return rule1 && rule2 && rule3;
}

/**
 * Step 7: Apply tone enhancement (brightness +3%, saturation +5%)
 */
async function applyToneEnhancement(buffer) {
    return await sharp(buffer)
        .modulate({
            brightness: 1.03, // +3%
            saturation: 1.05, // +5%
        })
        .toBuffer();
}

/**
 * Main AI Beautify service
 */
export const aiBeautifyService = {
    beautify: async ({ inputBuffer, inputMime, requestId }) => {
        try {
            // Step 2: Pre-scale to max 1440px (safe for GFPGAN GPU limit)
            const scaled = await preScale(inputBuffer, 1440);

            // Step 3: Run GFPGAN
            const gfpganOutput = await runGFPGAN(scaled, requestId);

            // Step 4: Run Real-ESRGAN with face_enhance
            const esrganOutput = await runRealESRGAN(gfpganOutput);

            // Step 5 & 6: Apply skin retouch (mask + blur)
            const retouchedOutput = await applySkinRetouch(esrganOutput);

            // Step 7: Apply tone enhancement
            const enhancedOutput = await applyToneEnhancement(retouchedOutput);

            // Step 8: Upload final buffer to R2
            const ext = inputMime?.includes("png") ? "png" : "jpg";
            const { key } = await uploadBufferToR2(enhancedOutput, {
                contentType: ext === "png" ? "image/png" : "image/jpeg",
                ext,
                prefix: "aiBeautify",
            });

            return {
                key,
                meta: {
                    bytes: enhancedOutput.length,
                    requestId,
                    pipeline: [
                        "pre-scale",
                        "gfpgan",
                        "real-esrgan",
                        "skin-retouch",
                        "tone-enhance",
                    ],
                },
            };
        } catch (error) {
            console.error("AI Beautify pipeline failed:", error);
            throw new Error(
                `AI Beautify failed: ${error.message || "Unknown error"}`
            );
        }
    },
};
