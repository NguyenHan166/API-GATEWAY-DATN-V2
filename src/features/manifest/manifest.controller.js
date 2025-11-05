import {
    getManifestCached,
    filterPacks,
    paginate,
} from "./manifest.service.js";
import { manifestQuerySchema, presignReqSchema } from "./manifest.schema.js";
import { presignGetUrl } from "../../integrations/r2/storage.service.js";

export async function getManifestController(req, res) {
    const q = manifestQuerySchema.parse(req.query);
    const data = await getManifestCached(); // {version, packs:[...]}
    const filtered = filterPacks(data, q.category, q.target);
    const [items, total, page, total_pages] = paginate(
        filtered,
        q.page,
        q.page_size
    );

    // Trả kiểu "ManifestPaged" tương tự Python
    res.json({
        version: data.version || "unknown",
        packs: items, // mảng pack: {id,title,category,target,files,count}
        total_packs: total,
        page,
        page_size: q.page_size,
        total_pages,
    });
}

export async function presignController(req, res) {
    const body = presignReqSchema.parse(req.body);
    const data = await getManifestCached();
    const pack = (data.packs || []).find((p) => p.id === body.pack_id);
    if (!pack) return res.status(404).json({ error: "Pack not found" });

    const found = (pack.files || []).some((f) => f.key === body.key);
    if (!found)
        return res.status(404).json({ error: "File not found in pack" });

    const EXPIRES = 3600; // hoặc lấy từ env/config
    const url = await presignGetUrl(body.key, EXPIRES);
    res.json({ url, expires_in: EXPIRES });
}
