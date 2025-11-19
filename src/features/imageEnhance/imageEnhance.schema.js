// Real-ESRGAN chỉ hỗ trợ scale 2 hoặc 4
const SCALE_OPTIONS = [2, 4];
const MODEL_OPTIONS = ["real-esrgan", "nightmareai/real-esrgan"];

function parseBoolean(value) {
    if (typeof value === "boolean") return value;
    if (typeof value === "string") {
        return ["1", "true", "yes", "on"].includes(value.toLowerCase());
    }
    return false;
}

export function validateEnhanceInput({
    scale,
    model,
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

    // Validate enhancement model (giữ field để backward compatibility)
    const m = String(model || "real-esrgan").toLowerCase();
    if (m && !MODEL_OPTIONS.includes(m)) {
        return {
            ok: false,
            error: `model không hợp lệ. Hỗ trợ: ${MODEL_OPTIONS.join(", ")}`,
        };
    }

    const fe = parseBoolean(faceEnhance);

    return {
        ok: true,
        value: {
            scale: s,
            model: "real-esrgan",
            faceEnhance: fe,
            fileBuffer,
            mimeType,
        },
    };
}

export const ALLOWED_SCALES = SCALE_OPTIONS;
export const ALLOWED_MODELS = MODEL_OPTIONS;
