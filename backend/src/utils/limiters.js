import pLimit from "p-limit";
import { PERF } from "../config/perf.js";

export const limits = {
    replaceBg: pLimit(PERF.concurrency.replaceBg),
    heavy: pLimit(PERF.concurrency.defaultHeavy),
};

// Helper cho feature mới:
export function makeLimiter(n = 4) {
    return pLimit(n);
}

/**
 * Concurrency cho các job nặng (Replicate, model lớn).
 * Bạn có thể đọc từ PERF.concurrency.replicate hoặc ENV nếu muốn.
 */
const MAX_CONCURRENCY = Number(process.env.REPLICATE_CONCURRENCY || 2);
const replicateLimiter = pLimit(Math.max(1, MAX_CONCURRENCY));

/**
 * Gói một tác vụ vào limiter cho Replicate.
 * Ví dụ: await withReplicateLimiter(() => doSomething());
 */
export async function withReplicateLimiter(taskFn) {
    return replicateLimiter(taskFn);
}
