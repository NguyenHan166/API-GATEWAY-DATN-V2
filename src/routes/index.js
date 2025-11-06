import { Router } from "express";
import manifestRoutes from "../features/manifest/manifest.routes.js";
import replaceRoutes from "../features/replaceBackground/replace.routes.js";
import replaceStyleRoutes from "../features/replaceStyle/replaceStyle.routes.js"; // +++

const api = Router();
api.use(manifestRoutes);
api.use(replaceRoutes);
api.use("/style", replaceStyleRoutes); // +++

export default api;
