// Real-ESRGAN only supports scale 2 or 4
const SCALE_OPTIONS = [2, 4];

export function validateClarityInput({
    scale,
    faceEnhance,
    fileBuffer,
    mimeType,
}) {
    if (!fileBuffer)
        return { ok: false, error: "Thiếu file 'image' (form-data)" };
    if (!mimeType?.startsWith("image/"))
        return { ok: false, error: "File không phải ảnh" };

    // Validate scale
    const s = parseInt(scale) || 2;
    if (!SCALE_OPTIONS.includes(s)) {
        return {
            ok: false,
            error: `scale không hợp lệ. Hỗ trợ: ${SCALE_OPTIONS.join(", ")}`,
        };
    }

    // Validate face enhance
    const fe = Boolean(faceEnhance);

    return {
        ok: true,
        value: {
            scale: s,
            faceEnhance: fe,
            fileBuffer,
            mimeType,
        },
    };
}

export const ALLOWED_SCALES = SCALE_OPTIONS;
