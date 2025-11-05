import { logger } from "../config/logger.js";

export function errorHandler(err, req, res, _next) {
    const status = err.statusCode || 500;
    logger.error({ err }, "Request error");
    res.status(status).json({
        error: err.message || "Internal Server Error",
        code: err.code || "INTERNAL_ERROR",
    });
}
