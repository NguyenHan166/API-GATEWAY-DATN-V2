import { styleService } from "./replaceStyle.service.js";
import { validateStyleInput } from "./replaceStyle.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";

export const styleController = {
    applyStyle: async (req, res) => {
        const { file } = req;
        const { style, extra } = req.body || {};

        const { ok, value, error } = validateStyleInput({
            style,
            extra,
            fileBuffer: file?.buffer,
            mimeType: file?.mimetype,
        });
        if (!ok)
            return res.status(400).json({ error: "BadRequest", detail: error });

        const { key, meta } = await styleService.applyStyle({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            style: value.style,
            extra: value.extra,
            requestId: req.id, // có sẵn từ middleware requestId của bạn
        });

        const expiresIn = 3600;
        const presignedUrl = await presignGetUrl(key, expiresIn); // 1h
        const publicUrl = buildPublicUrl(key);

        return res.json({
            key,
            url: publicUrl || presignedUrl,
            presigned_url: presignedUrl,
            expires_in: expiresIn,
            meta,
        });
    },
};
