import { PERF } from "../config/perf.js";

// Đặt timeout cho toàn request. Nếu quá thời gian, trả 504.
export function requestTimeout(req, res, next) {
    const ms = PERF.timeouts.requestMs;
    const timer = setTimeout(() => {
        if (!res.headersSent) {
            res.status(504).json({
                error: "Request timeout",
                request_id: req.id,
            });
        }
    }, ms);

    // clear khi kết thúc
    res.on("finish", () => clearTimeout(timer));
    res.on("close", () => clearTimeout(timer));
    next();
}
