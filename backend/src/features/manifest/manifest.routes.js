import { Router } from "express";
import rateLimit from "express-rate-limit";
import {
    getManifestController,
    presignController,
} from "./manifest.controller.js";

// giống limiter FastAPI:
const manifestLimiter = rateLimit({
    windowMs: 60_000,
    max: 30,
    standardHeaders: true,
});
const presignLimiter = rateLimit({
    windowMs: 60_000,
    max: 20,
    standardHeaders: true,
});

// (tùy) middleware kiểm tra API key
function requireApiKey(req, res, next) {
    // nếu bạn cần private: so sánh req.headers['x-api-key'] với env.API_KEY...
    // nếu public: bỏ qua
    return next();
}

const r = Router();
r.get("/manifest", requireApiKey, manifestLimiter, getManifestController);
r.post("/presign", requireApiKey, presignLimiter, presignController);

export default r;
