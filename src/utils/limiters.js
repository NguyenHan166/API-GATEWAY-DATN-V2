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
