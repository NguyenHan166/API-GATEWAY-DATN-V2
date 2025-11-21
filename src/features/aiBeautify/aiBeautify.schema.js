// src/features/aiBeautify/aiBeautify.schema.js

const DEFAULT_SCALE = 4;
const MIN_SCALE = 1;
const MAX_SCALE = 10;

function parseBoolean(raw) {
    if (raw === undefined || raw === null) return { ok: true, value: undefined };
    if (typeof raw === "boolean") return { ok: true, value: raw };

    const norm = String(raw).trim().toLowerCase();
    if (["1", "true", "yes", "y", "on"].includes(norm))
        return { ok: true, value: true };
    if (["0", "false", "no", "n", "off"].includes(norm))
        return { ok: true, value: false };

    return { ok: false, error: "face_enhance phải là boolean (true/false)" };
}

export function validateBeautifyInput({ fileBuffer, mimeType, body = {} }) {
    if (!fileBuffer) {
        return { ok: false, error: "Thiếu file 'image' (form-data)" };
    }

    if (!mimeType?.startsWith("image/")) {
        return { ok: false, error: "File không phải ảnh" };
    }

    // Check file size (max 10MB for processing)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (fileBuffer.length > maxSize) {
        return {
            ok: false,
            error: `Kích thước file quá lớn. Tối đa: ${
                maxSize / 1024 / 1024
            }MB`,
        };
    }

    let scale = DEFAULT_SCALE;
    if (body.scale !== undefined) {
        const n = Number(body.scale);
        if (!Number.isFinite(n)) {
            return { ok: false, error: "scale phải là số" };
        }
        if (n < MIN_SCALE || n > MAX_SCALE) {
            return {
                ok: false,
                error: `scale phải nằm trong khoảng ${MIN_SCALE}-${MAX_SCALE}`,
            };
        }
        scale = n;
    }

    const faceEnhanceRaw = body.face_enhance ?? body.faceEnhance;
    const parsedFaceEnhance = parseBoolean(faceEnhanceRaw);
    if (!parsedFaceEnhance.ok) {
        return parsedFaceEnhance;
    }
    const faceEnhance =
        parsedFaceEnhance.value === undefined
            ? true // default
            : parsedFaceEnhance.value;

    return {
        ok: true,
        value: {
            fileBuffer,
            mimeType,
            scale,
            faceEnhance,
        },
    };
}
