import path from "node:path";
import {
    INDEXED_PREFIXES,
    ALLOWED_EXTS,
    MANIFEST_CACHE_TTL_SECONDS,
} from "../../config/constants.js";
import {
    listObjects,
    headObject,
} from "../../integrations/r2/storage.service.js";

let _cache_manifest = null;
let _cache_time = 0;

function _extOf(key) {
    return path.extname(key || "").toLowerCase();
}
function _isValidFile(key) {
    return ALLOWED_EXTS.has(_extOf(key));
}
async function* _iterAllObjects(prefix) {
    let token = null;
    while (true) {
        const r = await listObjects(prefix, token);
        for (const it of r.Contents) yield it;
        if (r.IsTruncated) token = r.NextContinuationToken;
        else break;
    }
}

export async function buildManifest() {
    // Nhóm theo <Category>/<Target>/<File>
    const packs = {}; // pack_id -> {id,title,category,target,files[]}

    const prefixes =
        INDEXED_PREFIXES && INDEXED_PREFIXES.length ? INDEXED_PREFIXES : [""];
    for (const base of prefixes) {
        const basePrefix = base
            ? String(base).replace(/^\/+|\/+$/g, "") + "/"
            : "";
        for await (const obj of _iterAllObjects(basePrefix)) {
            const key = obj.Key;
            if (!key || key.endsWith("/")) continue;
            if (!_isValidFile(key)) continue;

            const parts = key.split("/");
            if (parts.length < 3) continue; // kỳ vọng: <Category>/<Target>/<File>
            const [category, target] = parts;

            const pack_id = `${category}/${target}`;
            const pack_title = `${category.replaceAll(
                "_",
                " "
            )} — ${target.replaceAll("_", " ")}`;

            if (!packs[pack_id]) {
                packs[pack_id] = {
                    id: pack_id,
                    title: pack_title,
                    category,
                    target,
                    files: [],
                };
            }

            const size = obj.Size || 0;
            const etag = (obj.ETag || "").replace(/"/g, "") || undefined;

            packs[pack_id].files.push({
                key,
                size,
                etag,
                content_type: "application/octet-stream",
            });
        }
    }

    const manifest = { version: "2025.10.0", packs: [] };
    for (const p of Object.values(packs)) {
        p.count = p.files.length;
        manifest.packs.push(p);
    }
    manifest.packs.sort((a, b) => {
        if (a.category === b.category) return a.target.localeCompare(b.target);
        return a.category.localeCompare(b.category);
    });
    return manifest;
}

export async function getManifestCached() {
    const now = Date.now() / 1000;
    if (!_cache_manifest || now - _cache_time > MANIFEST_CACHE_TTL_SECONDS) {
        _cache_manifest = await buildManifest();
        _cache_time = now;
        console.log(`[manifest] packs=${_cache_manifest.packs.length}`);
    }
    return _cache_manifest;
}

export function filterPacks(data, category, target) {
    let packs = data.packs || [];
    if (category) packs = packs.filter((p) => p.category === category);
    if (target) packs = packs.filter((p) => p.target === target);
    return packs;
}

export function paginate(items, page, page_size) {
    const total = items.length;
    const ps = page_size > 0 ? page_size : 50;
    const total_pages = total ? Math.max(1, Math.ceil(total / ps)) : 1;
    const p = Math.max(1, Math.min(page, total_pages));
    const start = (p - 1) * ps;
    const end = start + ps;
    return [items.slice(start, end), total, p, total_pages];
}
