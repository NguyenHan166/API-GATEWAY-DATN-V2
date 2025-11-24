# Cache & Metrics Guide

## 🎯 Quick Start

### 1. Monitoring Performance

Access the metrics endpoint to see real-time performance:

```bash
GET http://localhost:3000/metrics
```

**Response**:

```json
{
    "uptime": "3600s",
    "requests": {
        "total": 150,
        "success": 135,
        "error": 5,
        "cached": 40,
        "cacheHitRate": "26.67%",
        "errorRate": "3.33%"
    },
    "features": {
        "aiBeautify": {
            "count": 50,
            "avg": 25000,
            "min": 18000,
            "max": 45000,
            "p50": 23000,
            "p95": 38000,
            "p99": 42000,
            "errors": 2
        }
    },
    "cache": {
        "size": 45,
        "maxSize": 100,
        "ttlMs": 300000
    }
}
```

---

## 📊 Understanding Metrics

### Request Metrics

-   **total**: Total number of requests processed
-   **success**: Successfully completed requests
-   **error**: Failed requests
-   **cached**: Requests served from cache (instant response)
-   **cacheHitRate**: Percentage of requests served from cache
-   **errorRate**: Percentage of failed requests

### Timing Metrics (all in milliseconds)

-   **count**: Number of samples
-   **avg**: Average response time
-   **min/max**: Fastest/slowest response
-   **p50**: Median response time (50% faster than this)
-   **p95**: 95th percentile (95% faster than this)
-   **p99**: 99th percentile (99% faster than this)

### Cache Metrics

-   **size**: Current number of cached entries
-   **maxSize**: Maximum cache size (100 entries)
-   **ttlMs**: Time-to-live in milliseconds (5 minutes = 300000ms)

---

## 🔧 Cache Configuration

Cache is configured in `src/utils/cache.js`:

```js
export const resultCache = new SimpleCache(
    5 * 60 * 1000, // TTL: 5 minutes
    100 // Max size: 100 entries
);
```

### Adjusting Cache Settings

To change cache behavior, modify the values:

```js
// Longer TTL (10 minutes)
export const resultCache = new SimpleCache(10 * 60 * 1000, 100);

// Larger cache (200 entries)
export const resultCache = new SimpleCache(5 * 60 * 1000, 200);
```

---

## 🎯 Cache Hit Scenarios

### When Cache WILL Hit ✅

```
Request 1: Upload image.jpg, scale=2
→ Process (30s)

Request 2: Same image.jpg, scale=2 (within 5 min)
→ Cache hit (0ms) ⚡
```

### When Cache WON'T Hit ❌

```
Request 1: Upload image.jpg, scale=2
→ Process (30s)

Request 2: Same image.jpg, scale=4 (different params)
→ Cache miss, process again (30s)

Request 3: Same image.jpg, scale=2 (after 6 minutes)
→ Cache expired, process again (30s)
```

---

## 📈 Expected Performance

### Typical Cache Hit Rates

-   **Development**: 5-10% (testing different images)
-   **Production**: 15-30% (users retry similar images)
-   **High traffic**: Up to 40% (popular style transfers)

### Response Time Impact

-   **Without cache**: 20-45s (Replicate processing)
-   **With cache hit**: <50ms (instant from memory)
-   **Improvement**: 400-900x faster! ⚡

---

## 🚨 Monitoring & Alerts

### What to Watch

**Cache Hit Rate < 10%**:

-   ✅ Normal for development
-   ⚠️ Consider increasing TTL if production

**Error Rate > 5%**:

-   🔴 Investigate failures
-   Check Replicate API status
-   Review error logs

**P95 Latency > 60s**:

-   ⚠️ Slow responses for 5% of requests
-   Consider increasing heavy model concurrency
-   Check Replicate queue times

**Cache Size = MaxSize**:

-   ✅ Cache is full (good utilization)
-   Consider increasing maxSize if cache eviction is too frequent

---

## 🔍 Debugging Cache Issues

### Check if cache is working:

```bash
# Make same request twice
curl -X POST http://localhost:3000/api/ai-beautify \
  -F "image=@test.jpg" \
  -F "scale=2"

# Check metrics
curl http://localhost:3000/metrics | jq '.requests.cached'
# Should increment on second request
```

### Clear cache (requires code change):

```js
// In your service or route handler
import { resultCache } from "./utils/cache.js";

resultCache.clear(); // Clear all cache entries
```

---

## 💡 Best Practices

### For Developers

1. **Always check cache first** in new services:

    ```js
    const cacheKey = resultCache.makeKey(inputBuffer, params);
    const cached = resultCache.get(cacheKey);
    if (cached) return cached;
    ```

2. **Always store results**:

    ```js
    resultCache.set(cacheKey, result);
    ```

3. **Include all relevant params** in cache key:

    ```js
    // Good ✅
    makeKey(buffer, { scale, style, quality });

    // Bad ❌ - missing params
    makeKey(buffer, { scale });
    ```

### For Operations

1. **Monitor metrics regularly** - Set up alerts for error rate spikes
2. **Adjust cache size** based on memory availability
3. **Tune TTL** based on user behavior patterns
4. **Scale concurrency** based on Replicate plan limits

---

## 📚 Related Documentation

-   Main performance guide: `PERFORMANCE_OPTIMIZATION.md`
-   Environment variables: `.env.example`
-   API documentation: `API_DOCUMENTATION.md`
