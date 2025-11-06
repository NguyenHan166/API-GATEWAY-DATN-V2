const ALLOWED = ["anime", "ghibli", "watercolor", "oil-painting", "sketches"];

export function validateStyleInput({ style, extra, fileBuffer, mimeType }) {
    if (!fileBuffer)
        return { ok: false, error: "Thiếu file 'image' (form-data)" };
    if (!mimeType?.startsWith("image/"))
        return { ok: false, error: "File không phải ảnh" };
    const s = String(style || "").toLowerCase();
    if (!ALLOWED.includes(s)) {
        return {
            ok: false,
            error: "style không hợp lệ. Hỗ trợ: " + ALLOWED.join(", "),
        };
    }
    return {
        ok: true,
        value: {
            style: s,
            extra:
                typeof extra === "string" && extra.trim() ? extra.trim() : "",
            fileBuffer,
            mimeType,
        },
    };
}

export const ALLOWED_STYLES = ALLOWED;
