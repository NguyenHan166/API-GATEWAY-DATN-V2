import { Router } from "express";
import { upload } from "../../middlewares/upload.js";
import { asyncHandler } from "../../utils/asyncHandler.js";
import { replaceBgController } from "./replace.controller.js";

const r = Router();

r.post(
    "/replace-bg",
    upload.fields([
        { name: "fg", maxCount: 1 },
        { name: "bg", maxCount: 1 },
    ]),
    asyncHandler(replaceBgController)
);

export default r;
