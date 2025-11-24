/**
 * Simple metrics tracking for monitoring performance
 * Tracks: request counts, success/error rates, cache hits, response times
 */
export class SimpleMetrics {
    constructor() {
        this.requests = {
            total: 0,
            success: 0,
            error: 0,
            cached: 0,
        };

        // Feature -> array of durations (ms)
        this.timings = new Map();

        // Feature -> error count
        this.errors = new Map();

        this.startTime = Date.now();
    }

    /**
     * Record a request with timing and status
     * @param {string} feature - Feature name (e.g., 'aiBeautify', 'improveClarity')
     * @param {number} duration - Duration in milliseconds
     * @param {boolean} cached - Whether result was from cache
     * @param {boolean} error - Whether request failed
     */
    recordRequest(feature, duration, cached = false, error = false) {
        this.requests.total++;

        if (cached) {
            this.requests.cached++;
            this.requests.success++; // Cached is always success
        } else if (error) {
            this.requests.error++;
            const errorCount = this.errors.get(feature) || 0;
            this.errors.set(feature, errorCount + 1);
        } else {
            this.requests.success++;
        }

        // Record timing (only for non-errors)
        if (!error) {
            if (!this.timings.has(feature)) {
                this.timings.set(feature, []);
            }

            const timings = this.timings.get(feature);
            timings.push(duration);

            // Keep only last 100 timings per feature (rolling window)
            if (timings.length > 100) {
                timings.shift();
            }
        }
    }

    /**
     * Get statistics for a specific feature
     * @param {string} feature - Feature name
     * @returns {Object|null} - Stats or null if no data
     */
    getStats(feature) {
        const timings = this.timings.get(feature) || [];
        if (timings.length === 0) return null;

        const sorted = [...timings].sort((a, b) => a - b);
        const sum = timings.reduce((a, b) => a + b, 0);

        return {
            count: timings.length,
            avg: Math.round(sum / timings.length),
            min: sorted[0],
            max: sorted[sorted.length - 1],
            p50: sorted[Math.floor(sorted.length * 0.5)],
            p95: sorted[Math.floor(sorted.length * 0.95)],
            p99: sorted[Math.floor(sorted.length * 0.99)],
        };
    }

    /**
     * Get summary of all metrics
     * @returns {Object} - Complete metrics summary
     */
    getSummary() {
        const uptime = Math.floor((Date.now() - this.startTime) / 1000);
        const cacheHitRate =
            this.requests.total > 0
                ? ((this.requests.cached / this.requests.total) * 100).toFixed(
                      2
                  )
                : "0.00";

        const errorRate =
            this.requests.total > 0
                ? ((this.requests.error / this.requests.total) * 100).toFixed(2)
                : "0.00";

        const features = {};
        for (const [feature, _] of this.timings) {
            const stats = this.getStats(feature);
            if (stats) {
                features[feature] = {
                    ...stats,
                    errors: this.errors.get(feature) || 0,
                };
            }
        }

        return {
            uptime: `${uptime}s`,
            requests: {
                ...this.requests,
                cacheHitRate: `${cacheHitRate}%`,
                errorRate: `${errorRate}%`,
            },
            features,
        };
    }

    /**
     * Reset all metrics
     */
    reset() {
        this.requests = {
            total: 0,
            success: 0,
            error: 0,
            cached: 0,
        };
        this.timings.clear();
        this.errors.clear();
        this.startTime = Date.now();
    }
}

// Export singleton instance
export const metrics = new SimpleMetrics();
