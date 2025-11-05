import express from "express";
import helmet from "helmet";
import compression from "compression";
import api from "./routes/index.js";
import { errorHandler } from "./middlewares/error.js";
import { requestId } from "./middlewares/requestId.js";
import { requestTimeout } from "./middlewares/timeout.js";
import { PERF } from "./config/perf.js";

export function createApp() {
    const app = express();
    app.disable("x-powered-by");
    // app.set("trust proxy", true);

    // Bảo mật & hiệu năng
    app.use(helmet({ crossOriginResourcePolicy: false }));
    app.use(compression());
    app.use(requestId);
    app.use(requestTimeout);

    // Body parsers
    app.use(express.json({ limit: PERF.body.jsonLimit }));
    app.use(
        express.urlencoded({ extended: false, limit: PERF.body.jsonLimit })
    );

    // Health
    app.get("/health", (_req, res) => res.json({ ok: true }));

    // Routes
    app.use("/api", api);

    // Error handler
    app.use(errorHandler);
    return app;
}
