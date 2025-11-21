import { Router } from "express";
import rateLimit from "express-rate-limit";
import { upload } from "../../middlewares/upload.js";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { storyComicController } from "./storyComic.controller.js";

const generateLimiter = rateLimit({
    windowMs: 60_000,
    max: 6,
    standardHeaders: true,
});

const router = Router();

router.post(
    "/story-comic/generate",
    generateLimiter,
    upload.none(),
    asyncHandler(storyComicController.generate)
);

export default router;
