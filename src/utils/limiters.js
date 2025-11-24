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
 * Replicate concurrency limiters by model type
 * - light: Fast models (Real-ESRGAN, GFPGAN) - 8 concurrent
 * - heavy: Slow models (IC-Light, FLUX) - 4 concurrent
 * - comic: Story generation (Gemini + Animagine) - 2 concurrent
 */
const REPLICATE_LIMITS = {
    light: pLimit(Math.max(1, PERF.concurrency.replicateLight)),
    heavy: pLimit(Math.max(1, PERF.concurrency.replicateHeavy)),
    comic: pLimit(Math.max(1, PERF.concurrency.replicateComic)),
};

/**
 * Wrap a task with Replicate limiter by model type
 * @param {Function} taskFn - Async function to execute
 * @param {string} type - Model type: 'light' | 'heavy' | 'comic' (default: 'light')
 * @returns {Promise} - Task result
 *
 * @example
 * // Light models (fast)
 * await withReplicateLimiter(() => replicate.run(MODEL, ...), 'light');
 *
 * // Heavy models (slow)
 * await withReplicateLimiter(() => replicate.run(MODEL, ...), 'heavy');
 */
export async function withReplicateLimiter(taskFn, type = "light") {
    const limiter = REPLICATE_LIMITS[type] || REPLICATE_LIMITS.light;
    return limiter(taskFn);
}
