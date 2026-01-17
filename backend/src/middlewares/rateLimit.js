import rateLimit from "express-rate-limit";
import { ipKeyGenerator } from "express-rate-limit";

/**
 * Rate limit theo route (IP-based) nhưng:
 * - Nếu có API key/Bearer token thì dùng key đó để gom nhóm
 * - Nếu fallback theo IP thì bắt buộc dùng ipKeyGenerator để an toàn IPv6
 *
 * @param {object} opts
 * @param {number} opts.windowMs - cửa sổ thời gian (ms)
 * @param {number} opts.max - số request tối đa trong cửa sổ
 * @param {string} opts.key - nhãn route để đưa vào key
 * @param {number|false} opts.ipv6Subnet - subnet cho IPv6 (mặc định 64, theo lib)
 */
export function rateLimitPerRoute({
    windowMs = 60_000,
    max = 60,
    key = "route",
    ipv6Subnet = 64,
} = {}) {
    return rateLimit({
        windowMs,
        max,
        standardHeaders: true,
        legacyHeaders: false,

        // Gom theo API key/token nếu có; nếu không có thì gom theo IP (dùng ipKeyGenerator!)
        keyGenerator: (req, _res) => {
            const apiKey =
                req.headers["x-api-key"] ||
                req.headers["x-client-key"] ||
                (typeof req.get === "function"
                    ? req.get("authorization")
                    : undefined);

            // Ưu tiên Bearer/API key để không "đánh đồng" người dùng thật sau proxy/NAT
            if (apiKey && typeof apiKey === "string" && apiKey.trim()) {
                return `${key}:ak:${apiKey.trim()}`;
            }

            // BẮT BUỘC: dùng helper để chuẩn hoá IPv6, tránh bypass
            // ipKeyGenerator(req.ip, ipv6Subnet) -> IPv4 giữ nguyên, IPv6 trả CIDR subnet
            const ipNorm = ipKeyGenerator(req.ip || "", ipv6Subnet);
            return `${key}:ip:${ipNorm}`;
        },

        handler: (req, res /*, next*/) => {
            res.status(429).json({
                error: "TooManyRequests",
                detail: `Rate limit for ${key}`,
            });
        },
    });
}
