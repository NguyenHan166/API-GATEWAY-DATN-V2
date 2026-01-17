import { Router } from "express";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { upload } from "../../middlewares/upload.js";
import { clarityController } from "./improveClarity.controller.js";
import { rateLimitPerRoute } from "../../middlewares/rateLimit.js";

const router = Router();

// Limit route này: 60 req / 1 phút / IP
router.use(rateLimitPerRoute({ windowMs: 60_000, max: 60, key: "clarity" }));

// POST /api/clarity
// form-data:
//   - image (file) - required
//   - scale (number) - 2 | 4, default: 2
//   - faceEnhance (boolean) - default: false
router.post(
    "/",
    upload.single("image"),
    asyncHandler(clarityController.improve)
);

export default router;
