import { Router } from "express";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { upload } from "../../middlewares/upload.js";
import { styleController } from "./replaceStyle.controller.js";
import { rateLimitPerRoute } from "../../middlewares/rateLimit.js";

const router = Router();

// Limit route này: 60 req / 1 phút / IP (tùy chỉnh trong perf hoặc env)
router.use(rateLimitPerRoute({ windowMs: 60_000, max: 60, key: "style" }));

// POST /api/style
// form-data: image (file), style (string), extra (optional string)
router.post(
    "/",
    upload.single("image"),
    asyncHandler(styleController.applyStyle)
);

export default router;
