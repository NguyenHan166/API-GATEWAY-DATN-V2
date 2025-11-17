// src/features/gfpgan/gfpgan.controller.js
import { gfpganService } from "./gfpgan.service.js";
import { validateGfpganInput } from "./gfpgan.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { successResponse, errorResponse } from "../../utils/response.js";

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

        const { key, meta } = await gfpganService.enhance({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            scale: value.scale,
            version: value.version,
            requestId: req.id,
        });

        const expiresIn = 3600; // 1h
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
