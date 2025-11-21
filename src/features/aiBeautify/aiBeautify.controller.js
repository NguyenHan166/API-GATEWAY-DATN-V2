// src/features/aiBeautify/aiBeautify.controller.js
import { aiBeautifyService } from "./aiBeautify.service.js";
import { validateBeautifyInput } from "./aiBeautify.schema.js";
import {
    presignGetUrl,
    buildPublicUrl,
} from "../../integrations/r2/storage.service.js";
import { successResponse, errorResponse } from "../../utils/response.js";

export const aiBeautifyController = {
    beautify: async (req, res) => {
        const { file } = req;

        const { ok, value, error } = validateBeautifyInput({
            fileBuffer: file?.buffer,
            mimeType: file?.mimetype,
            body: req.body,
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

        const { key, meta } = await aiBeautifyService.beautify({
            inputBuffer: value.fileBuffer,
            inputMime: value.mimeType,
            scale: value.scale,
            faceEnhance: value.faceEnhance,
            requestId: req.id,
        });

        const expiresIn = 3600;
        const presignedUrl = await presignGetUrl(key, expiresIn);
        const publicUrl = buildPublicUrl(key);

        // Format giống các features khác
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
