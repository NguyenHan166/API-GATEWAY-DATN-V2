import { parseGenerateBody } from "./comic.schema.js";
import { generateComic } from "./comic.service.js";
import { errorResponse } from "../../utils/response.js";

export const comicController = {
    generate: async (req, res) => {
        const parsed = parseGenerateBody(req.body || {});
        if (!parsed.ok) {
            return res.status(400).json(
                errorResponse({
                    requestId: req.id,
                    error: "Invalid input",
                    code: "VALIDATION_ERROR",
                    details: parsed.error,
                })
            );
        }

        try {
            const result = await generateComic({
                prompt: parsed.value.prompt,
                panels: parsed.value.panels,
                style: parsed.value.style,
                requestId: req.id,
            });

            return res.json({
                request_id: req.id,
                status: "success",
                page_url: result.page.url,
                data: {
                    key: result.page.key,
                    url: result.page.url,
                    presigned_url: result.page.presigned_url,
                },
                meta: {
                    story_id: result.story_id,
                    panels: result.meta.panels,
                    model: result.meta.model,
                },
            });
        } catch (err) {
            return res.status(400).json(
                errorResponse({
                    requestId: req.id,
                    error: err?.message || "Failed to generate comic",
                    code: "PROCESSING_ERROR",
                })
            );
        }
    },
};
