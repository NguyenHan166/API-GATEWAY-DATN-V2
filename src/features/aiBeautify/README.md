# AI Beautify Feature

## Overview

-   Face-focused upscaling/beautification powered by `alexgenovese/upscaler` (GFPGAN-based) on Replicate
-   Default 4x upscale with face enhancement enabled
-   Supports JPEG/PNG/WebP up to 10MB
-   Outputs are stored under the `aiBeautify/` prefix in Cloudflare R2

## Pipeline

1. Validate input (mime, size)
2. Run `alexgenovese/upscaler:4f7eb3…` with:
    - `scale`: 1-10 (default `4`)
    - `face_enhance`: boolean (default `true`)
3. Upload result to R2 and return presigned/public URLs

## API Endpoint

`POST /api/ai-beautify`

### Request (multipart/form-data)

| Field            | Type  | Required | Notes                                |
| ---------------- | ----- | -------- | ------------------------------------ |
| `image`          | File  | Yes      | JPEG/PNG/WebP, max 10MB              |
| `scale`          | Number| No       | 1-10, defaults to 4                  |
| `face_enhance`   | Bool  | No       | `true`/`false`, defaults to `true`   |

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
        "model": "alexgenovese/upscaler",
        "version": "4f7eb3da655b5182e559d50a0437440f242992d47e5e20bd82829a79dee61ff3",
        "scale": 4,
        "faceEnhance": true,
        "bytes": 245678,
        "requestId": "abc123",
        "pipeline": ["alexgenovese/upscaler"]
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

-   Replicate model typically finishes in ~4-12 seconds (depends on input size/scale)
-   Concurrency guarded by `withReplicateLimiter`

## Quick Start (cURL)

```bash
curl -X POST http://localhost:3000/api/ai-beautify \
  -F "image=@./portrait.jpg" \
  -F "scale=4" \
  -F "face_enhance=true"
```

## Dependencies

-   `replicate`: AI inference
-   `@aws-sdk/client-s3`: R2 storage
-   `p-limit`: Concurrency control
3. Retry logic handles transient failures
4. Efficient buffer reuse (no temp files)
