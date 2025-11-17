import { Router } from "express";
import manifestRoutes from "../features/manifest/manifest.routes.js";
import replaceRoutes from "../features/replaceBackground/replace.routes.js";
import replaceStyleRoutes from "../features/replaceStyle/replaceStyle.routes.js"; // +++
import upscale from "../features/gfpgan/gfpgan.routes.js";
import portraitsRoutes from "../features/portraits/portraits.routes.js";
import clarityRoutes from "../features/improveClarity/improveClarity.routes.js";
import enhanceRoutes from "../features/imageEnhance/imageEnhance.routes.js";
const api = Router();
api.use(manifestRoutes);
api.use(replaceRoutes);
api.use("/style", replaceStyleRoutes); // +++
api.use("/upscale", upscale);
api.use("/portraits", portraitsRoutes);
api.use("/clarity", clarityRoutes);
api.use("/enhance", enhanceRoutes);
export default api;
