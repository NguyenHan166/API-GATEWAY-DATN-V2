import express from "express";
import helmet from "helmet";
import compression from "compression";
import api from "./routes/index.js";
import { errorHandler } from "./middlewares/error.js";
import { requestId } from "./middlewares/requestId.js";
import { PERF } from "./config/perf.js";
import { getManifestCached } from "./features/manifest/manifest.service.js";

export function createApp() {
    const app = express();
    app.disable("x-powered-by");
    // Behind Railway's single proxy; trust only the first hop (safer than `true`)
    app.set("trust proxy", 1);

    // Bảo mật & hiệu năng
    app.use(helmet({ crossOriginResourcePolicy: false }));
    app.use(compression());
    app.use(requestId);

    // Body parsers
    app.use(express.json({ limit: PERF.body.jsonLimit }));
    app.use(
        express.urlencoded({ extended: false, limit: PERF.body.jsonLimit })
    );

    // Health
    app.get("/health", (_req, res) => res.json({ ok: true }));

    // Routes
    app.use("/api", api);

    getManifestCached({ fresh: true })
        .then(() => console.log("[startup] manifest warmed"))
        .catch((e) => console.error("[startup] warm manifest failed:", e));

    // Error handler
    app.use(errorHandler);
    return app;
}
