# AI Beautify API Documentation

## Endpoint

```
POST /api/ai-beautify
```

## Description

Advanced portrait enhancement pipeline that combines multiple AI models:

1. **GFPGAN** - Face restoration and detail enhancement
2. **Real-ESRGAN** - Super-resolution and face enhancement
3. **Skin Retouch** - Intelligent skin smoothing
4. **Tone Enhancement** - Professional color grading

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter | Type | Required | Description                  |
| --------- | ---- | -------- | ---------------------------- |
| `image`   | File | Yes      | Image file (JPEG, PNG, WebP) |

### Constraints

-   **Max file size**: 10MB
-   **Supported formats**: JPEG, PNG, WebP
-   **Recommended**: Portrait photos with visible faces
-   **Auto pre-scaling**: Images larger than 2048px will be scaled down

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "aiBeautify/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://your-public-url.com/aiBeautify/...",
        "expires_in": 3600
    },
    "meta": {
        "bytes": 245678,
        "requestId": "req_abc123xyz",
        "pipeline": [
            "pre-scale",
            "gfpgan",
            "real-esrgan",
            "skin-retouch",
            "tone-enhance"
        ]
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
        "details": "Thiếu file 'image' (form-data)"
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

| Field             | Type   | Description                                    |
| ----------------- | ------ | ---------------------------------------------- |
| `request_id`      | String | Unique request identifier for tracking         |
| `status`          | String | "success" or "error"                           |
| `data.key`        | String | R2 storage key for the enhanced image          |
| `data.url`        | String | Public URL or presigned URL                    |
| `data.expires_in` | Number | URL expiration time in seconds (3600 = 1 hour) |
| `meta.bytes`      | Number | Output file size in bytes                      |
| `meta.pipeline`   | Array  | List of processing steps applied               |

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

### 1. Pre-Scale (Automatic)

-   Max dimension: 1440px (safe for GFPGAN GPU limit)
-   Preserves aspect ratio
-   Quality: 92% JPEG
-   **Purpose**: Optimize processing time and stay within GPU memory limits
-   **Note**: GFPGAN has ~2.09M pixel limit (1440x1440 fits safely)

### 2. GFPGAN Face Restoration

-   **Model**: tencentarc/gfpgan v1.4
-   **Scale**: 2x
-   **Features**:
    -   Restores facial details
    -   Fixes blur and compression artifacts
    -   Enhances facial features
    -   Removes minor blemishes

### 3. Real-ESRGAN Enhancement

-   **Model**: nightmareai/real-esrgan
-   **Scale**: 2x (then resized to original)
-   **Face Enhance**: Enabled
-   **Features**:
    -   Overall image enhancement
    -   Face-aware processing
    -   Detail preservation
    -   Noise reduction

### 4. Skin Retouch

-   **Method**: Color-based segmentation + selective blur
-   **Blur Sigma**: 1.4
-   **Features**:
    -   Detects skin tones using HSV analysis
    -   Applies gentle blur only to skin
    -   Preserves eyes, hair, clothing details
    -   Natural-looking results

### 5. Tone Enhancement

-   **Brightness**: +3%
-   **Saturation**: +5%
-   **Features**:
    -   Subtle color boost
    -   Professional color grading
    -   Enhanced vibrancy
    -   Balanced exposure

## Examples

### cURL

```bash
curl -X POST http://localhost:3000/api/ai-beautify \
  -F "image=@./portrait.jpg" \
  -H "Accept: application/json"
```

### JavaScript (Fetch API)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);

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

response = requests.post(url, files=files)
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

const response = await fetch("http://localhost:3000/api/ai-beautify", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(result);
```

## Performance

### Expected Processing Time

-   **Small images** (< 1MP): 30-45 seconds
-   **Medium images** (1-4MP): 45-60 seconds
-   **Large images** (> 4MP): 60-90 seconds

_Note: Processing time depends on Replicate API load and image complexity_

### Optimization Tips

1. Pre-resize images client-side to ~1080p for faster processing
2. Use JPEG format instead of PNG when possible
3. Compress images before upload (quality 90-95%)
4. Batch process multiple images during off-peak hours

## Error Codes

| Code                  | HTTP Status | Description                | Solution                            |
| --------------------- | ----------- | -------------------------- | ----------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   | Check file format and size          |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry after specified time |
| `INTERNAL_ERROR`      | 500         | Server processing error    | Retry or contact support            |
| `REPLICATE_ERROR`     | 500         | AI model processing failed | Retry with different image          |

## Best Practices

### Image Quality

✅ **DO:**

-   Use high-quality source images (at least 512x512px)
-   Ensure good lighting in original photo
-   Use JPEG format for photos
-   Center the face in the frame
-   Images will auto-scale to max 1440px

❌ **DON'T:**

-   Upload extremely low-resolution images (< 256px)
-   Use heavily compressed JPEGs
-   Process images with multiple faces (optimized for single portraits)
-   Upload images with extreme aspect ratios
-   Expect images > 1440px to maintain original resolution

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
-   Sequential pipeline ensures quality
-   Background processing recommended for batch operations

### Storage

-   Enhanced images stored in Cloudflare R2
-   Automatic key generation with timestamp
-   Prefix: `aiBeautify/YYYY-MM-DD/`
-   Format: JPEG (optimized) or PNG (if source is PNG)

### Monitoring

-   All requests include unique `requestId`
-   Pipeline steps tracked in metadata
-   Error context preserved for debugging

## Support

For issues or questions:

1. Check the README.md in the feature folder
2. Review error messages and request IDs
3. Ensure Replicate API key is configured
4. Verify R2 storage credentials

## Changelog

### v1.0.0 (2025-11-18)

-   Initial release
-   GFPGAN + Real-ESRGAN pipeline
-   Skin retouch with color-based segmentation
-   Tone enhancement
-   R2 storage integration
-   Rate limiting (30 req/min)
