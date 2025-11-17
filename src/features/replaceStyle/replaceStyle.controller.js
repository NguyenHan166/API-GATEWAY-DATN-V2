import { styleService } from "./replaceStyle.service.js";
import { validateStyleInput } from "./replaceStyle.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { successResponse, errorResponse } from "../../utils/response.js";

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
        if (!ok) {
            return res.status(400).json(
                errorResponse({
                    requestId: req.id,
                    error: "Invalid input",
                    code: "VALIDATION_ERROR",
                    details: error,
                })
            );
        }

        const { key, meta } = await styleService.applyStyle({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            style: value.style,
            extra: value.extra,
            requestId: req.id,
        });

        const expiresIn = 3600;
        const presignedUrl = await presignGetUrl(key, expiresIn);
        const publicUrl = buildPublicUrl(key);

        return res.json(
            successResponse({
                requestId: req.id,
                key,
                url: publicUrl,
                presignedUrl,
                expiresIn,
                meta,
            })
        );
    },
};
