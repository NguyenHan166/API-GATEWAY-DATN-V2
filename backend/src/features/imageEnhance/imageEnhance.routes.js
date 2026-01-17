import { Router } from "express";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { upload } from "../../middlewares/upload.js";
import { enhanceController } from "./imageEnhance.controller.js";
import { rateLimitPerRoute } from "../../middlewares/rateLimit.js";

const router = Router();

// Limit route này: 60 req / 1 phút / IP
router.use(rateLimitPerRoute({ windowMs: 60_000, max: 60, key: "enhance" }));

// POST /api/enhance
// form-data:
//   - image (file) - required
//   - scale (number) - 2 | 4, default: 2
//   - face_enhance (boolean) - optional, improve faces when true
//   - model (string) - optional, only "real-esrgan" (backward compatibility)
router.post(
    "/",
    upload.single("image"),
    asyncHandler(enhanceController.enhance)
);

export default router;
