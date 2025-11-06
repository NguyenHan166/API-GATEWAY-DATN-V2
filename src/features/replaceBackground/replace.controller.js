import {
    uploadBufferToR2,
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { replaceBackground } from "./replace.service.js";
import { replaceQuerySchema } from "./replace.schema.js";
import { limits } from "../../utils/limiters.js";

export async function replaceBgController(req, res) {
    const task = async () => {
        const fgBuf = req.files?.fg?.[0]?.buffer;
        const bgBuf = req.files?.bg?.[0]?.buffer;
        if (!fgBuf || !bgBuf)
            return res.status(400).json({ error: "Thiếu file fg hoặc bg" });

        const q = replaceQuerySchema.parse(req.body);
        const { finalPng, W, H } = await replaceBackground({
            fgBuf,
            bgBuf,
            fit: q.fit ?? "cover",
            position: q.position ?? "centre",
            featherPx: q.featherPx ?? 1,
            addShadow: (q.shadow ?? "1") === "1",
        });

        const { key } = await uploadBufferToR2(finalPng, {
            contentType: "image/png",
            ext: "png",
        });
        const expiresIn = q.signTtl ?? 3600;
        const publicUrl = buildPublicUrl(key);
        const presignedUrl = await presignGetUrl(key, expiresIn);

        res.json({
            key,
            url: publicUrl || presignedUrl,
            presigned_url: presignedUrl,
            expires_in: expiresIn,
            meta: { width: W, height: H },
        });
    };

    // Hạn chế đồng thời theo p-limit, các request dư sẽ xếp hàng ngắn trong process
    return limits.replaceBg(task);
}
