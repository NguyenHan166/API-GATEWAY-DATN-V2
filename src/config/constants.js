// tuỳ bạn, đây là ví dụ
export const MANIFEST_CACHE_TTL_SECONDS = 60; // cache 60s
export const ALLOWED_EXTS = new Set([
    ".xmp",
    ".cube",
    ".png",
    ".jpg",
    ".jpeg",
    ".webp",
    ".bmp",
    ".tif",
    ".tiff",
    ".gif",
    ".zip",
]);
// nếu muốn giới hạn vùng duyệt trong bucket: ["ON1_BW_LUTs/", "SomethingElse/"]
export const INDEXED_PREFIXES = []; // [] = duyệt toàn bucket
