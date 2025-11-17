// src/features/aiBeautify/aiBeautify.schema.js

export function validateBeautifyInput({ fileBuffer, mimeType }) {
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

    return {
        ok: true,
        value: {
            fileBuffer,
            mimeType,
        },
    };
}
