import { logger } from "../config/logger.js";
import { errorResponse } from "../utils/response.js";

export function errorHandler(err, req, res, _next) {
    const status = err.statusCode || 500;
    logger.error({ err, requestId: req.id }, "Request error");

    res.status(status).json(
        errorResponse({
            requestId: req.id,
            error: err.message || "Internal Server Error",
            code: err.code || "INTERNAL_ERROR",
            details: err.details || null,
        })
    );
}
