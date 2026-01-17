# AI Beautify Feature

## Overview

-   High-quality image super-resolution powered by `cjwbw/real-esrgan` on Replicate
-   Real-ESRGAN: Real-World Blind Super-Resolution for enhancing image details
-   Default 2x upscale, supports 2-4x
-   Supports JPEG/PNG/WebP up to 10MB
-   Outputs are stored under the `aiBeautify/` prefix in Cloudflare R2

## Pipeline

1. Validate input (mime, size)
2. Auto-resize if image > ~2MP (~1414x1414 max) to fit GPU memory
3. Run `cjwbw/real-esrgan:42fed1c4...` with:
    - `scale`: 2-4 (default `2`)
4. Upload result to R2 and return presigned/public URLs

## API Endpoint

`POST /api/ai-beautify`

### Request (multipart/form-data)

| Field          | Type   | Required | Notes                               |
| -------------- | ------ | -------- | ----------------------------------- |
| `image`        | File   | Yes      | JPEG/PNG/WebP, max 10MB             |
| `scale`        | Number | No       | 2-4, defaults to 2                  |
| `face_enhance` | Bool   | No       | `true`/`false`, defaults to `false` |

### Response

```json
{
    "request_id": "abc123",
    "status": "success",
    "data": {
        "key": "aiBeautify/2025-11-18/uuid.jpg",
        "url": "https://your-public-url.com/aiBeautify/2025-11-18/uuid.jpg",
        "expires_in": 3600
    },
    "meta": {
        "model": "cjwbw/real-esrgan",
        "version": "42fed1c4974146d4d2414e2be2c5277c7fcf05fcc3a73abf41610695738c1d7b",
        "scale": 2,
        "bytes": 245678,
        "requestId": "abc123",
        "pipeline": ["cjwbw/real-esrgan"]
    }
}
```

## Rate Limiting

-   30 requests per minute per IP (stricter because Replicate jobs are GPU-bound)

## Error Handling

-   Validates file type and size
-   Validates `scale` range and `face_enhance` boolean
-   Retries transient Replicate failures with backoff
-   Returns requestId in errors for tracing

## Performance

-   Replicate model typically finishes in ~10-15 seconds (depends on input size/scale)
-   Scale 2 is faster than scale 4
-   Concurrency guarded by `withReplicateLimiter`

## Quick Start (cURL)

```bash
curl -X POST http://localhost:3000/api/ai-beautify \
  -F "image=@./portrait.jpg" \
  -F "scale=2"
```

## Dependencies

-   `replicate`: AI inference
-   `@aws-sdk/client-s3`: R2 storage
-   `p-limit`: Concurrency control

3. Retry logic handles transient failures
4. Efficient buffer reuse (no temp files)
