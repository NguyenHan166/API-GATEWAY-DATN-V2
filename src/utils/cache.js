import crypto from "crypto";

/**
 * Simple in-memory cache with TTL and LRU eviction
 * Perfect for caching Replicate API results
 */
export class SimpleCache {
    constructor(ttlMs = 300000, maxSize = 100) {
        this.cache = new Map();
        this.ttlMs = ttlMs; // Default 5 minutes
        this.maxSize = maxSize;
    }

    /**
     * Generate cache key from buffer + params + requestId
     * @param {Buffer} buffer - Image buffer
     * @param {Object} params - Request parameters (scale, style, etc.)
     * @param {string} requestId - Request ID for cache isolation
     * @returns {string} - Cache key (16-char hash)
     */
    makeKey(buffer, params = {}, requestId = null) {
        if (!requestId) {
            console.warn(
                "[Cache] WARNING: No requestId provided - cache may leak between requests"
            );
            requestId = `fallback_${Date.now()}_${Math.random()}`;
        }
        const hash = crypto
            .createHash("sha256")
            .update(buffer)
            .update(JSON.stringify(params))
            .update(requestId)
            .digest("hex");
        return hash.slice(0, 16);
    }

    /**
     * Get cached value if exists and not expired
     * @param {string} key - Cache key
     * @returns {any|null} - Cached value or null
     */
    get(key) {
        const item = this.cache.get(key);
        if (!item) return null;

        // Check if expired
        if (Date.now() - item.timestamp > this.ttlMs) {
            this.cache.delete(key);
            return null;
        }

        // Update access time for LRU
        item.lastAccess = Date.now();
        return item.value;
    }

    /**
     * Set cache value with automatic LRU eviction
     * @param {string} key - Cache key
     * @param {any} value - Value to cache
     */
    set(key, value) {
        // Simple LRU: delete oldest if size > maxSize
        if (this.cache.size >= this.maxSize) {
            // Find oldest entry by lastAccess
            let oldestKey = null;
            let oldestTime = Infinity;

            for (const [k, v] of this.cache.entries()) {
                const accessTime = v.lastAccess || v.timestamp;
                if (accessTime < oldestTime) {
                    oldestTime = accessTime;
                    oldestKey = k;
                }
            }

            if (oldestKey) {
                this.cache.delete(oldestKey);
            }
        }

        this.cache.set(key, {
            value,
            timestamp: Date.now(),
            lastAccess: Date.now(),
        });
    }

    /**
     * Check if key exists and not expired
     * @param {string} key - Cache key
     * @returns {boolean}
     */
    has(key) {
        return this.get(key) !== null;
    }

    /**
     * Clear all cache entries
     */
    clear() {
        this.cache.clear();
    }

    /**
     * Get cache statistics
     * @returns {Object} - Stats object
     */
    get stats() {
        return {
            size: this.cache.size,
            maxSize: this.maxSize,
            ttlMs: this.ttlMs,
        };
    }
}

// Export singleton instance for image processing results
// TTL: 5 minutes (enough for duplicate requests)
// MaxSize: 100 entries (balance between memory and hit rate)
export const resultCache = new SimpleCache(5 * 60 * 1000, 100);
