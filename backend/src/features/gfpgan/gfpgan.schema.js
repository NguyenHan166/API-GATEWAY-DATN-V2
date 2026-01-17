// src/features/gfpgan/gfpgan.schema.js
const ALLOWED = ["image/jpeg", "image/png", "image/webp"];

export function validateGfpganInput({ fileBuffer, mimeType, scale, version }) {
    if (!fileBuffer)
        return { ok: false, error: "Thiếu file 'image' (form-data)" };
    if (!mimeType?.startsWith("image/") || !ALLOWED.includes(mimeType)) {
        return {
            ok: false,
            error: "File không phải ảnh hợp lệ (jpeg/png/webp)",
        };
    }

    const s = Number(scale);
    const v = String(version || "v1.4").trim(); // "v1.3" hoặc "v1.4"

    return {
        ok: true,
        value: {
            fileBuffer,
            mimeType,
            scale: Number.isFinite(s) && [1, 2, 4].includes(s) ? s : 2,
            version: v,
        },
    };
}
