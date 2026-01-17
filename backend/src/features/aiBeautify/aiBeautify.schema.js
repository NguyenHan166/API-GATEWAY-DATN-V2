// src/features/aiBeautify/aiBeautify.schema.js

const DEFAULT_SCALE = 2;
const MIN_SCALE = 2;
const MAX_SCALE = 4;

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

    return {
        ok: true,
        value: {
            fileBuffer,
            mimeType,
            scale,
        },
    };
}
