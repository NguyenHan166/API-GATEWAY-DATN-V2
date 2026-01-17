// middlewares/timeout.js
/**
 * Middleware timeout có thể cấu hình theo route.
 * - Mặc định dùng PERF.timeouts.httpMs
 * - Cho phép override bằng res.locals.timeoutMs
 * - Không hủy nếu response đã gửi header/body
 */
import { PERF } from "../config/perf.js";

export function requestTimeout(req, res, next) {
    const tMs = Number(res.locals?.timeoutMs || PERF.timeouts.httpMs || 30000);
    if (!Number.isFinite(tMs) || tMs <= 0) return next();

    const timer = setTimeout(() => {
        if (res.headersSent) return;
        res.status(504).json({ error: "Request timeout", request_id: req.id });
        // quan trọng: kết thúc response, không gọi next()
    }, tMs);

    const clear = () => clearTimeout(timer);
    res.on("finish", clear);
    res.on("close", clear);
    res.on("error", clear);
    next();
}
