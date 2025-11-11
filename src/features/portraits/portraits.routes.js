// ESM
import express from "express";
import multer from "multer";
import { portraitsController } from "./portraits.controller.js";

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

// POST /api/portraits (preset: ic-light)
router.post(
    "/ic-light",
    upload.single("image"), // form-data: image (file)
    portraitsController.icLight
);

export default router;
