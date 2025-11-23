import {
    uploadBufferToR2,
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { removeBackground, replaceBackground } from "./replace.service.js";
import { replaceQuerySchema } from "./replace.schema.js";
import { limits } from "../../utils/limiters.js";
import { successResponse, errorResponse } from "../../utils/response.js";

export async function replaceBgController(req, res) {
    const task = async () => {
        const fgBuf = req.files?.fg?.[0]?.buffer;
        if (!fgBuf) {
            return res.status(400).json(
                errorResponse({
                    requestId: req.id,
                    error: "Missing required file",
                    code: "MISSING_FILE",
                    details: "fg file is required",
                })
            );
        }

        const q = replaceQuerySchema.parse(req.body);
        const mode = q.mode || "replace";

        let finalPng, W, H;

        if (mode === "remove") {
            // Chế độ remove background - chỉ cần fg
            const result = await removeBackground({
                fgBuf,
                featherPx: q.featherPx ?? 1,
            });
            finalPng = result.finalPng;
            W = result.W;
            H = result.H;
        } else {
            // Chế độ replace background - cần cả fg và bg
            const bgBuf = req.files?.bg?.[0]?.buffer;
            if (!bgBuf) {
                return res.status(400).json(
                    errorResponse({
                        requestId: req.id,
                        error: "Missing required file",
                        code: "MISSING_FILE",
                        details: "bg file is required for replace mode",
                    })
                );
            }

            const result = await replaceBackground({
                fgBuf,
                bgBuf,
                fit: q.fit ?? "cover",
                position: q.position ?? "centre",
                featherPx: q.featherPx ?? 1,
                addShadow: (q.shadow ?? "1") === "1",
            });
            finalPng = result.finalPng;
            W = result.W;
            H = result.H;
        }

        const { key } = await uploadBufferToR2(finalPng, {
            contentType: "image/png",
            ext: "png",
        });
        const expiresIn = q.signTtl ?? 3600;
        const publicUrl = buildPublicUrl(key);
        const presignedUrl = await getImageUrl(key, expiresIn);

        res.json(
            successResponse({
                requestId: req.id,
                key,
                url: publicUrl,
                presignedUrl,
                expiresIn,
                meta: { width: W, height: H, mode },
            })
        );
    };

    // Hạn chế đồng thời theo p-limit, các request dư sẽ xếp hàng ngắn trong process
    return limits.replaceBg(task);
}
