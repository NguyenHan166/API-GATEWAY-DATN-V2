// Retry đơn giản với backoff
export async function withRetry(
    fn,
    { retries = 3, baseDelayMs = 500, factor = 2, beforeRetry } = {}
) {
    let attempt = 0;
    let lastErr;
    while (attempt <= retries) {
        try {
            return await fn();
        } catch (err) {
            lastErr = err;
            if (attempt === retries) break;
            const delay = baseDelayMs * Math.pow(factor, attempt);
            if (beforeRetry) {
                try {
                    await beforeRetry({ attempt, delay, err });
                } catch {}
            }
            await new Promise((r) => setTimeout(r, delay));
            attempt++;
        }
    }
    throw lastErr;
}
