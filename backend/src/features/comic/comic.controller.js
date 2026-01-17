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
                pages: parsed.value.pages,
                panelsPerPage: parsed.value.panelsPerPage,
                style: parsed.value.style,
                requestId: req.id,
            });

            return res.json({
                request_id: req.id,
                status: "success",
                comic_url: result.image.url,
                data: {
                    comic_id: result.comic_id,
                    image: result.image,
                    panels: result.panels,
                },
                meta: result.meta,
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
