// src/features/gfpgan/gfpgan.controller.js
import { gfpganService } from "./gfpgan.service.js";
import { validateGfpganInput } from "./gfpgan.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";

export const gfpganController = {
    enhance: async (req, res) => {
        const { file } = req;
        const { scale, version } = req.body || {};

        const { ok, value, error } = validateGfpganInput({
            fileBuffer: file?.buffer,
            mimeType: file?.mimetype,
            scale,
            version,
        });
        if (!ok)
            return res.status(400).json({ error: "BadRequest", detail: error });

        const { key, meta } = await gfpganService.enhance({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            scale: value.scale,
            version: value.version,
            requestId: req.id,
        });

        const expiresIn = 3600; // 1h
        const presignedUrl = await presignGetUrl(key, expiresIn);
        const publicUrl = buildPublicUrl(key); // vẫn trả kèm như module mẫu

        return res.json({
            key,
            url: publicUrl || presignedUrl,
            presigned_url: presignedUrl,
            expires_in: expiresIn,
            meta,
        });
    },
};
