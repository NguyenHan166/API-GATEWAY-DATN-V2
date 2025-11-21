# AI Beautify API Documentation

## Endpoint

```
POST /api/ai-beautify
```

## Description

Face-focused upscaling and restoration powered by `alexgenovese/upscaler` (GFPGAN-based) on Replicate. Defaults to 4x upscale with face enhancement enabled; override `scale` (1-10) and `face_enhance` as needed.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter      | Type   | Required | Description                                   |
| -------------- | ------ | -------- | --------------------------------------------- |
| `image`        | File   | Yes      | Image file (JPEG, PNG, WebP)                  |
| `scale`        | Number | No       | 1-10, defaults to 4                           |
| `face_enhance` | Bool   | No       | `true`/`false`, defaults to `true`            |

### Constraints

-   **Max file size**: 10MB
-   **Supported formats**: JPEG, PNG, WebP
-   **Recommended**: Portrait photos with visible faces
-   Scale > 6 may increase processing time

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
        "model": "alexgenovese/upscaler",
        "version": "4f7eb3da655b5182e559d50a0437440f242992d47e5e20bd82829a79dee61ff3",
        "scale": 4,
        "faceEnhance": true,
        "bytes": 245678,
        "requestId": "req_abc123xyz",
        "pipeline": ["alexgenovese/upscaler"]
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
        "details": "scale phải nằm trong khoảng 1-10"
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

| Field               | Type   | Description                                    |
| ------------------- | ------ | ---------------------------------------------- |
| `request_id`        | String | Unique request identifier for tracking         |
| `status`            | String | "success" or "error"                           |
| `data.key`          | String | R2 storage key for the enhanced image          |
| `data.url`          | String | Public URL or presigned URL                    |
| `data.presigned_url`| String | Presigned URL when returned                    |
| `data.expires_in`   | Number | URL expiration time in seconds (3600 = 1 hour) |
| `meta.model`        | String | `alexgenovese/upscaler`                        |
| `meta.version`      | String | Pinned model version hash                      |
| `meta.scale`        | Number | Scale passed to the model                      |
| `meta.faceEnhance`  | Bool   | Whether face enhancement was enabled           |
| `meta.bytes`        | Number | Output file size in bytes                      |
| `meta.pipeline`     | Array  | Processing steps (single model)                |

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

1. Validate input (mime, size, `scale`, `face_enhance`)
2. Run `alexgenovese/upscaler:4f7eb3...` on Replicate
    - `scale`: 1-10 (default 4)
    - `face_enhance`: boolean (default true)
3. Upload to Cloudflare R2 (`aiBeautify/YYYY-MM-DD/uuid.ext`)
4. Return presigned & public URLs

## Examples

### cURL

```bash
curl -X POST http://localhost:3000/api/ai-beautify \
  -F "image=@./portrait.jpg" \
  -F "scale=4" \
  -F "face_enhance=true" \
  -H "Accept: application/json"
```

### JavaScript (Fetch API)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "4");
formData.append("face_enhance", "true");

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
data = {'scale': '3', 'face_enhance': 'true'}

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
form.append("scale", "4");
form.append("face_enhance", "true");

const response = await fetch("http://localhost:3000/api/ai-beautify", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(result);
```

## Performance

### Expected Processing Time

-   **Typical**: ~4-12 seconds (depends on input size and scale)
-   Higher `scale` or large inputs can add a few extra seconds

_Note: Processing time depends on Replicate API load_

### Optimization Tips

1. Keep `scale` at 4-6 for balanced quality & speed
2. Use JPEG format for photos with good lighting
3. Run heavier jobs during off-peak hours to reduce queueing

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
-   Ensure good lighting and a clear face
-   Use JPEG format for photos
-   Keep `scale` in the 4-6 range for best fidelity

❌ **DON'T:**

-   Upload extremely low-resolution images (<256px)
-   Expect sharp results with motion blur or heavy noise
-   Send unrealistic `scale` values (>10 is rejected)
-   Ignore rate limit responses

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

1. **Profile Pictures**

    - Social media avatars
    - Professional networking photos
    - Dating app profiles

2. **E-commerce**

    - Model photography enhancement
    - Product photos with people
    - Lifestyle images

3. **Event Photography**

    - Wedding photos
    - Corporate headshots
    - Party/celebration photos

4. **Content Creation**
    - YouTube thumbnails
    - Blog author photos
    - Podcast cover art

## Technical Notes

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

### v2.0.0 (2025-xx-xx)

-   Switched pipeline to `alexgenovese/upscaler` (GFPGAN) on Replicate
-   Added `scale` and `face_enhance` controls
-   Simplified single-model pipeline with version pinning

### v1.0.0 (2025-11-18)

-   Initial release
-   GFPGAN + Real-ESRGAN pipeline with skin retouch
-   R2 storage integration and rate limiting (30 req/min)
