import { Router } from "express";
import rateLimit from "express-rate-limit";
import { comicController } from "./comic.controller.js";
import { upload } from "../../middlewares/upload.js";

const generateLimiter = rateLimit({
    windowMs: 60_000,
    max: 10,
    standardHeaders: true,
});

const r = Router();
r.post(
    "/comic/generate",
    generateLimiter,
    upload.none(), // accept multipart/form-data with text fields
    comicController.generate
);

export default r;
