// Topaz Labs supports scale 2, 4, or 6
const SCALE_OPTIONS = [2, 4, 6];

// Topaz Labs enhancement models
const ENHANCEMENT_MODELS = {
    "standard-v2": "Standard V2 - General purpose",
    "low-res-v2": "Low Resolution V2 - For low-res images",
    cgi: "CGI - For digital art",
    "high-fidelity-v2": "High Fidelity V2 - Preserves details",
    "text-refine": "Text Refine - Optimized for text",
};

export function validateEnhanceInput({ scale, model, fileBuffer, mimeType }) {
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

    // Validate enhancement model
    const m = String(model || "standard-v2").toLowerCase();
    if (!ENHANCEMENT_MODELS[m]) {
        return {
            ok: false,
            error: `model không hợp lệ. Hỗ trợ: ${Object.keys(
                ENHANCEMENT_MODELS
            ).join(", ")}`,
        };
    }

    return {
        ok: true,
        value: {
            scale: s,
            model: m,
            fileBuffer,
            mimeType,
        },
    };
}

export const ALLOWED_SCALES = SCALE_OPTIONS;
export const ALLOWED_MODELS = ENHANCEMENT_MODELS;
