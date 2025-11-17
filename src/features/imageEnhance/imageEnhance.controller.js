import { enhanceService } from "./imageEnhance.service.js";
import { validateEnhanceInput } from "./imageEnhance.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { successResponse, errorResponse } from "../../utils/response.js";

export const enhanceController = {
    enhance: async (req, res) => {
        const { file } = req;
        const { scale, model } = req.body || {};

        const { ok, value, error } = validateEnhanceInput({
            scale,
            model,
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

        const { key, meta } = await enhanceService.enhanceImage({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            scale: value.scale,
            model: value.model,
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
