import { Router } from "express";
import { upload } from "../../middlewares/upload.js";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { replaceBgController } from "./replace.controller.js";
import { rateLimitPerRoute } from "../../middlewares/rateLimit.js";

const r = Router();

r.use((req, res, next) => {
    res.locals.timeoutMs = 180_000;
    next();
});

r.use(rateLimitPerRoute({ windowMs: 60_000, max: 60, key: "style" }));

r.post(
    "/replace-bg",
    upload.fields([
        { name: "fg", maxCount: 1 },
        { name: "bg", maxCount: 1 },
    ]),
    asyncHandler(replaceBgController)
);

export default r;
