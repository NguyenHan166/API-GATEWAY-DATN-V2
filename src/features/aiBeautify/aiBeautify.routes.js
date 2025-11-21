// src/features/aiBeautify/aiBeautify.routes.js
import { Router } from "express";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { upload } from "../../middlewares/upload.js";
import { aiBeautifyController } from "./aiBeautify.controller.js";
import { rateLimitPerRoute } from "../../middlewares/rateLimit.js";

const router = Router();

// Limit route này: 30 req / 1 phút / IP (stricter because it's resource-intensive)
router.use(rateLimitPerRoute({ windowMs: 60_000, max: 30, key: "aiBeautify" }));

/**
 * POST /api/ai-beautify
 * form-data:
 *   - image (file) - required
 *   - scale (number, optional, default 4)
 *   - face_enhance (boolean, optional, default true)
 *
 * Response: Presigned URL to enhanced image
 */
router.post(
    "/",
    upload.single("image"),
    asyncHandler(aiBeautifyController.beautify)
);

export default router;
