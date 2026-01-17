import { clarityService } from "./improveClarity.service.js";
import { validateClarityInput } from "./improveClarity.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
    getImageUrl,
} from "../../integrations/r2/storage.service.js";
import { successResponse, errorResponse } from "../../utils/response.js";

export const clarityController = {
    improve: async (req, res) => {
        const { file } = req;
        const { scale, faceEnhance } = req.body || {};

        const { ok, value, error } = validateClarityInput({
            scale,
            faceEnhance,
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

        const { key, meta } = await clarityService.improveClarity({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            scale: value.scale,
            faceEnhance: value.faceEnhance,
            requestId: req.id,
        });

        const expiresIn = 3600;
        const presignedUrl = await getImageUrl(key, expiresIn);
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
