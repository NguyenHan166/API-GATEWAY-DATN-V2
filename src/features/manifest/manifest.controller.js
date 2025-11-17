import {
    getManifestCached,
    filterPacks,
    paginate,
} from "./manifest.service.js";
import { manifestQuerySchema, presignReqSchema } from "./manifest.schema.js";
import { presignGetUrl } from "../../integrations/r2/storage.service.js";
import { paginatedResponse, errorResponse } from "../../utils/response.js";

export async function getManifestController(req, res) {
    const q = manifestQuerySchema.parse(req.query);
    const data = await getManifestCached(); // {version, packs:[...]}
    const filtered = filterPacks(data, q.category, q.target);
    const [items, total, page, total_pages] = paginate(
        filtered,
        q.page,
        q.page_size
    );

    res.json(
        paginatedResponse({
            requestId: req.id,
            items,
            total,
            page,
            pageSize: q.page_size,
            totalPages: total_pages,
            meta: {
                version: data.version || "unknown",
            },
        })
    );
}

export async function presignController(req, res) {
    const body = presignReqSchema.parse(req.body);
    const data = await getManifestCached();
    const pack = (data.packs || []).find((p) => p.id === body.pack_id);
    if (!pack) {
        return res.status(404).json(
            errorResponse({
                requestId: req.id,
                error: "Pack not found",
                code: "NOT_FOUND",
            })
        );
    }

    const found = (pack.files || []).some((f) => f.key === body.key);
    if (!found) {
        return res.status(404).json(
            errorResponse({
                requestId: req.id,
                error: "File not found in pack",
                code: "NOT_FOUND",
            })
        );
    }

    const EXPIRES = 3600;
    const url = await presignGetUrl(body.key, EXPIRES);

    res.json({
        request_id: req.id,
        status: "success",
        data: {
            url,
            expires_in: EXPIRES,
        },
    });
}
