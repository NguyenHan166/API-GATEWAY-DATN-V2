import { parseStoryComicBody } from "./storyComic.schema.js";
import { generateStoryComic } from "./storyComic.service.js";
import { errorResponse } from "../../utils/response.js";
import { logger } from "../../config/logger.js";

export const storyComicController = {
    generate: async (req, res) => {
        const parsed = parseStoryComicBody(req.body || {});
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
            const result = await generateStoryComic({
                prompt: parsed.value.prompt,
                pages: parsed.value.pages,
                panelsPerPage: parsed.value.panels_per_page,
                styleSelector: parsed.value.style_selector,
                qualitySelector: parsed.value.quality_selector,
                requestId: req.id,
            });

            return res.json({
                request_id: req.id,
                status: "success",
                story_id: result.storyId,
                pages: result.pages,
                meta: result.meta,
            });
        } catch (err) {
            logger.error(
                {
                    requestId: req.id,
                    err: err?.message,
                },
                "Failed to generate story comic"
            );

            return res.status(400).json(
                errorResponse({
                    requestId: req.id,
                    error: err?.message || "Failed to generate story comic",
                    code: "PROCESSING_ERROR",
                })
            );
        }
    },
};
