import { parseIcLightBody } from "./portraits.schema.js";
import { icLightRelight } from "./relight.service.js";
import {
    successResponseMultiple,
    errorResponse,
} from "../../utils/response.js";

export const portraitsController = {
    icLight: async (req, res) => {
        const requestId = req.id;
        try {
            // ép số nếu là string (do form-data)
            const coerceNum = (v) => (v === undefined ? undefined : Number(v));
            const body = {
                ...req.body,
                steps: coerceNum(req.body?.steps),
                cfg: coerceNum(req.body?.cfg),
                width: coerceNum(req.body?.width),
                height: coerceNum(req.body?.height),
                number_of_images: coerceNum(req.body?.number_of_images),
                output_quality: coerceNum(req.body?.output_quality),
            };

            const parsed = parseIcLightBody(body);

            const output = await icLightRelight({
                fileBuffer: req.file?.buffer || null,
                imageUrl: parsed.image_url || null,
                prompt: parsed.prompt,
                appended_prompt: parsed.appended_prompt,
                negative_prompt: parsed.negative_prompt,
                light_source: parsed.light_source,
                steps: parsed.steps,
                cfg: parsed.cfg,
                width: parsed.width,
                height: parsed.height,
                number_of_images: parsed.number_of_images,
                output_format: parsed.output_format,
                output_quality: parsed.output_quality,
                requestId,
            });

            // Format lại output để phù hợp với response chuẩn
            const outputs = output.output_urls.map((url, index) => ({
                url,
                index,
            }));

            res.json(
                successResponseMultiple({
                    requestId,
                    outputs,
                    meta: output.meta,
                })
            );
        } catch (err) {
            res.status(400).json(
                errorResponse({
                    requestId,
                    error: err?.message || "Bad Request",
                    code: "PROCESSING_ERROR",
                })
            );
        }
    },
};
