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
//   - scale (number) - 2 | 4 | 6, default: 2
//   - model (string) - "standard-v2" | "low-res-v2" | "cgi" | "high-fidelity-v2" | "text-refine", default: "standard-v2"
router.post(
    "/",
    upload.single("image"),
    asyncHandler(enhanceController.enhance)
);

export default router;
