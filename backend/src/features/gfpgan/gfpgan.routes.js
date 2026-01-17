// src/features/gfpgan/gfpgan.routes.js
import { Router } from "express";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { upload } from "../../middlewares/upload.js";
import { gfpganController } from "./gfpgan.controller.js";
import { rateLimitPerRoute } from "../../middlewares/rateLimit.js";

const router = Router();

// ví dụ: 60 req/phút/IP cho route này
router.use(rateLimitPerRoute({ windowMs: 60_000, max: 60, key: "gfpgan" }));

// POST /api/gfpgan
// form-data: image (file), scale (1|2|4), version (default "v1.4")
router.post(
    "/",
    upload.single("image"),
    asyncHandler(gfpganController.enhance)
);

export default router;
