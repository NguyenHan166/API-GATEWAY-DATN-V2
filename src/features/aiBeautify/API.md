# AI Beautify API Documentation

## Endpoint

```
POST /api/ai-beautify
```

## Description

High-quality image super-resolution powered by `cjwbw/real-esrgan` on Replicate. Real-ESRGAN enhances image quality and details while removing artifacts. Defaults to 2x upscale; supports scale factors of 2-4.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter      | Type   | Required | Description                         |
| -------------- | ------ | -------- | ----------------------------------- |
| `image`        | File   | Yes      | Image file (JPEG, PNG, WebP)        |
| `scale`        | Number | No       | 2-4, defaults to 2                  |
| `face_enhance` | Bool   | No       | `true`/`false`, defaults to `false` |

### Constraints

-   **Max file size**: 10MB
-   **Supported formats**: JPEG, PNG, WebP
-   **Scale factors**: 2, 3, or 4 only
-   Scale 4 may increase processing time significantly

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "aiBeautify/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://your-public-url.com/aiBeautify/...",
        "presigned_url": "https://pub-xxxx.r2.dev/aiBeautify/...",
        "expires_in": 3600
    },
    "meta": {
        "model": "cjwbw/real-esrgan",
        "version": "42fed1c4974146d4d2414e2be2c5277c7fcf05fcc3a73abf41610695738c1d7b",
        "scale": 2,
        "bytes": 245678,
        "requestId": "req_abc123xyz",
        "pipeline": ["cjwbw/real-esrgan"]
    }
}
```

### Error Response (400 Bad Request)

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "Invalid input",
        "code": "VALIDATION_ERROR",
        "details": "scale phải nằm trong khoảng 2-4"
    }
}
```

### Error Response (500 Internal Server Error)

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "AI Beautify failed: Prediction failed",
        "code": "INTERNAL_ERROR"
    }
}
```

## Response Fields

| Field                | Type   | Description                                    |
| -------------------- | ------ | ---------------------------------------------- |
| `request_id`         | String | Unique request identifier for tracking         |
| `status`             | String | "success" or "error"                           |
| `data.key`           | String | R2 storage key for the enhanced image          |
| `data.url`           | String | Public URL or presigned URL                    |
| `data.presigned_url` | String | Presigned URL when returned                    |
| `data.expires_in`    | Number | URL expiration time in seconds (3600 = 1 hour) |
| `meta.model`         | String | `cjwbw/real-esrgan`                            |
| `meta.version`       | String | Pinned model version hash                      |
| `meta.scale`         | Number | Scale passed to the model                      |
| `meta.bytes`         | Number | Output file size in bytes                      |
| `meta.pipeline`      | Array  | Processing steps (single model)                |

## Rate Limiting

-   **Limit**: 30 requests per minute per IP address
-   **Window**: 60 seconds
-   **Response**: 429 Too Many Requests

```json
{
    "success": false,
    "error": "Too many requests",
    "code": "RATE_LIMIT_EXCEEDED",
    "retryAfter": 42
}
```

## Processing Pipeline

1. Validate input (mime, size, `scale`)
2. Run `cjwbw/real-esrgan:42fed1c...` on Replicate
    - `scale`: 2-4 (default 2)
3. Upload to Cloudflare R2 (`aiBeautify/YYYY-MM-DD/uuid.ext`)
4. Return presigned & public URLs

## Examples

### cURL

```bash
curl -X POST http://localhost:3000/api/ai-beautify \
  -F "image=@./portrait.jpg" \
  -F "scale=2" \
  -H "Accept: application/json"
```

### JavaScript (Fetch API)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "2");

const response = await fetch("http://localhost:3000/api/ai-beautify", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Enhanced image URL:", result.data.url);
```

### Python (requests)

```python
import requests

url = "http://localhost:3000/api/ai-beautify"
files = {'image': open('portrait.jpg', 'rb')}
data = {'scale': '2'}

response = requests.post(url, files=files, data=data)
result = response.json()

print(f"Enhanced image: {result['data']['url']}")
```

### Node.js (FormData)

```javascript
import fs from "fs";
import FormData from "form-data";
import fetch from "node-fetch";

const form = new FormData();
form.append("image", fs.createReadStream("./portrait.jpg"));
form.append("scale", "2");

const response = await fetch("http://localhost:3000/api/ai-beautify", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(result);
```

## Performance

### Expected Processing Time

-   **Typical**: ~10-15 seconds (depends on input size and scale)
-   Scale 4 processing may take longer than scale 2
-   Real-ESRGAN is optimized for real-world images

_Note: Processing time depends on Replicate API load_

### Optimization Tips

1. Use scale 2 for faster processing, scale 4 for maximum quality
2. Use JPEG format for photos to reduce upload time
3. Consider input image resolution - very large images take longer
4. Run during off-peak hours to reduce queueing

## Error Codes

| Code                  | HTTP Status | Description                | Solution                            |
| --------------------- | ----------- | -------------------------- | ----------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   | Check file format/size and params   |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry after specified time |
| `INTERNAL_ERROR`      | 500         | Server processing error    | Retry or contact support            |
| `REPLICATE_ERROR`     | 500         | AI model processing failed | Retry with different image          |

## Best Practices

### Image Quality

✅ **DO:**

-   Use high-quality source images (≥512px on the short side)
-   Use JPEG format for photos
-   Start with scale 2, increase to 4 if needed for maximum quality

❌ **DON'T:**

-   Upload extremely low-resolution images (<256px)
-   Use scale values outside 2-4 range (will be rejected)
-   Ignore rate limit responses
-   Expect miracles from heavily compressed/damaged images

### API Usage

✅ **DO:**

-   Implement retry logic with exponential backoff
-   Handle rate limits gracefully
-   Cache results using the `key` field
-   Use `requestId` for debugging

❌ **DON'T:**

-   Repeatedly process the same image
-   Ignore rate limit responses
-   Store presigned URLs long-term (they expire!)
-   Skip error handling

## Use Cases

1. **Image Enhancement**

    - Upscale low-resolution photos
    - Restore old/degraded images
    - Improve social media images
    - Enhance digital art

2. **E-commerce**

    - Product photography enhancement
    - Catalog image upscaling
    - Detail improvement for zoom views

3. **Content Creation**

    - YouTube thumbnails
    - Blog images
    - Marketing materials
    - Print preparation

4. **Photography**
    - Portrait enhancement (with face_enhance)
    - Landscape upscaling
    - Event photography improvement

## Technical Notes

### Model Information

-   **Model**: `cjwbw/real-esrgan`
-   **Version**: `42fed1c4974146d4d2414e2be2c5277c7fcf05fcc3a73abf41610695738c1d7b`
-   **Hardware**: Nvidia T4 GPU
-   **Cost**: ~$0.0032 per run
-   **Base Algorithm**: Real-ESRGAN (Real-World Blind Super-Resolution)

### Concurrency

-   Processing uses `withReplicateLimiter` to prevent API overload
-   Each request runs a single Replicate prediction

### Storage

-   Enhanced images stored in Cloudflare R2
-   Automatic key generation with timestamp
-   Prefix: `aiBeautify/YYYY-MM-DD/`
-   Format: JPEG (optimized) or PNG (if source is PNG)

### Monitoring

-   All requests include unique `requestId`
-   Model version + params tracked in metadata
-   Error context preserved for debugging

## Support

For issues or questions:

1. Check the README.md in the feature folder
2. Review error messages and request IDs
3. Ensure Replicate API key is configured
4. Verify R2 storage credentials

## Changelog

### v3.0.0 (2025-11-23)

-   Migrated to `cjwbw/real-esrgan` for improved super-resolution quality
-   Changed scale range to 2-4 (from 1-10)
-   Changed default scale to 2 (from 4)
-   Changed face_enhance default to false (from true)
-   Real-ESRGAN optimized for real-world images with better artifact handling

### v2.0.0 (2025-xx-xx)

-   Switched pipeline to `alexgenovese/upscaler` (GFPGAN) on Replicate
-   Added `scale` and `face_enhance` controls
-   Simplified single-model pipeline with version pinning

### v1.0.0 (2025-11-18)

-   Initial release
-   GFPGAN + Real-ESRGAN pipeline with skin retouch
-   R2 storage integration and rate limiting (30 req/min)
