import { Router } from "express";
import rateLimit from "express-rate-limit";
import { comicController } from "./comic.controller.js";

const generateLimiter = rateLimit({
    windowMs: 60_000,
    max: 10,
    standardHeaders: true,
});

const r = Router();
r.post("/comic/generate", generateLimiter, comicController.generate);

export default r;
