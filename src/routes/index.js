import { Router } from "express";
import manifestRoutes from "../features/manifest/manifest.routes.js";
import replaceRoutes from "../features/replaceBackground/replace.routes.js";
import replaceStyleRoutes from "../features/replaceStyle/replaceStyle.routes.js"; // +++
import beautifyRoutes from "../features/gfpgan/gfpgan.routes.js";
import portraitsRoutes from "../features/portraits/portraits.routes.js";
const api = Router();
api.use(manifestRoutes);
api.use(replaceRoutes);
api.use("/style", replaceStyleRoutes); // +++
api.use("/auto-beautify", beautifyRoutes);
api.use("/portraits", portraitsRoutes);
export default api;
