# Real-ESRGAN Clarity Improvement API Documentation

## Overview

Real-ESRGAN là AI model chuyên về super-resolution và tăng độ sắc nét cho ảnh. Service này sử dụng model từ nightmareai/real-esrgan để:

-   Tăng độ phân giải ảnh (upscaling)
-   Cải thiện độ sắc nét và chi tiết
-   Giảm nhiễu và artifact
-   Tùy chọn enhance khuôn mặt

## Endpoint

```
POST /api/clarity
```

## Description

Upload ảnh và nhận về ảnh đã được tăng độ sắc nét và upscale bằng Real-ESRGAN AI model.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter     | Type    | Required | Description                      | Default |
| ------------- | ------- | -------- | -------------------------------- | ------- |
| `image`       | File    | ✅       | File ảnh (JPEG, PNG, WebP, etc.) | -       |
| `scale`       | Number  | ❌       | Hệ số scale (2 hoặc 4)           | `2`     |
| `faceEnhance` | Boolean | ❌       | Bật face enhancement             | `false` |

### Constraints

-   **Max file size**: 10MB (theo config upload middleware)
-   **Supported formats**: JPEG, PNG, WebP và hầu hết các format ảnh
-   **Auto-resize**: Images larger than ~2MP (~1414x1414) will be automatically resized to fit GPU memory
-   **Scale options**:
    -   `2`: Tăng 2x kích thước (recommended)
    -   `4`: Tăng 4x kích thước

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "clarity/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://pub-xxxx.r2.dev/clarity/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "presigned_url": "https://pub-xxxx.r2.dev/clarity/...?X-Amz-Algorithm=...",
        "expires_in": 3600
    },
    "meta": {
        "model": "nightmareai/real-esrgan",
        "scale": 2,
        "face_enhance": false,
        "prescaled": false,
        "input_size": {
            "width": 1024,
            "height": 768
        },
        "output_size": {
            "width": 2048,
            "height": 1536
        }
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
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
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### Error Response (500 Internal Server Error)

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "Clarity improvement failed",
        "code": "INTERNAL_ERROR"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

## Response Fields

| Field                | Type    | Description                          |
| -------------------- | ------- | ------------------------------------ |
| `request_id`         | String  | Unique request identifier            |
| `status`             | String  | "success" or "error"                 |
| `data.key`           | String  | R2 storage key                       |
| `data.url`           | String  | Public URL (if available)            |
| `data.presigned_url` | String  | Presigned URL for secure access      |
| `data.expires_in`    | Number  | Presigned URL expiration (seconds)   |
| `meta.model`         | String  | AI model used                        |
| `meta.scale`         | Number  | Scale factor applied                 |
| `meta.face_enhance`  | Boolean | Whether face enhancement was enabled |
| `meta.prescaled`     | Boolean | Whether input was auto-resized       |
| `meta.originalSize`  | Object  | Original dimensions (if prescaled)   |
| `meta.prescaledSize` | Object  | Resized dimensions (if prescaled)    |
| `meta.input_size`    | Object  | Original image dimensions            |
| `meta.output_size`   | Object  | Enhanced image dimensions            |

## Rate Limiting

-   **Limit**: 60 requests per minute per IP
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

## Examples

### Basic Usage (2x upscale)

#### cURL

```bash
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@./photo.jpg"
```

#### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);

const response = await fetch("http://localhost:3000/api/clarity", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Enhanced image URL:", result.data.url);
console.log("Output size:", result.meta.output_size);
```

#### Python

```python
import requests

url = "http://localhost:3000/api/clarity"
files = {'image': open('photo.jpg', 'rb')}

response = requests.post(url, files=files)
result = response.json()

print(f"Enhanced image: {result['data']['url']}")
```

### Advanced Usage (4x scale + face enhance)

#### cURL

```bash
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@./portrait.jpg" \
  -F "scale=4" \
  -F "faceEnhance=true"
```

#### JavaScript

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "4");
formData.append("faceEnhance", "true");

const response = await fetch("http://localhost:3000/api/clarity", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("4x enhanced with face:", result.data.url);
```

#### Python

```python
import requests

files = {'image': open('portrait.jpg', 'rb')}
data = {
    'scale': '4',
    'faceEnhance': 'true'
}

response = requests.post(
    'http://localhost:3000/api/clarity',
    files=files,
    data=data
)

result = response.json()
print(f"Output: {result['data']['url']}")
print(f"Face enhance: {result['meta']['face_enhance']}")
```

### Node.js (FormData)

```javascript
import fs from "fs";
import FormData from "form-data";
import fetch from "node-fetch";

const form = new FormData();
form.append("image", fs.createReadStream("./photo.jpg"));
form.append("scale", "2");
form.append("faceEnhance", "false");

const response = await fetch("http://localhost:3000/api/clarity", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(result);
```

## Processing Details

### How Real-ESRGAN Works

1. **Image Analysis**: Analyzes input image structure and details
2. **Super-Resolution**: Uses deep learning to generate high-res version
3. **Detail Enhancement**: Adds realistic details and textures
4. **Noise Reduction**: Removes compression artifacts and noise
5. **Face Enhancement** (optional): Extra processing for facial features
6. **Post-processing**: Refines and outputs final image

### Processing Time

-   **Small images** (< 1MP): 20-35 seconds
-   **Medium images** (1-4MP): 35-60 seconds
-   **Large images** (> 4MP): 60-120 seconds
-   **4x scale**: Add 30-50% more time

_Note: Face enhancement adds approximately 20-30% to processing time_

## Use Cases

### Photography Enhancement

-   Upscale old photos
-   Improve scanned images
-   Enhance low-resolution photos
-   Restore image clarity

### Social Media

-   Prepare images for print
-   Upscale for large displays
-   Improve photo quality for posts
-   Enhance profile pictures

### E-commerce

-   Upscale product photos
-   Enhance catalog images
-   Prepare for high-res displays
-   Improve zoom quality

### Digital Art

-   Upscale digital artwork
-   Improve game textures
-   Enhance illustrations
-   Prepare for large formats

## Face Enhancement

### When to Use

✅ **Enable Face Enhancement:**

-   Portrait photos
-   Group photos
-   Photos with visible faces
-   Headshots and profile pictures

❌ **Don't Enable:**

-   Landscape photos
-   Abstract images
-   Photos without faces
-   When you want faster processing

### How It Works

Face enhancement applies additional AI processing specifically for facial features:

-   Sharper eyes and facial details
-   Better skin texture
-   Enhanced facial structure
-   More natural-looking results

## Scale Selection Guide

### Scale 2 (Recommended)

**Best for:**

-   General use cases
-   Balance between quality and file size
-   Faster processing
-   Most images

**Output:**

-   1000x1000 → 2000x2000
-   File size: ~2-3x larger

### Scale 4

**Best for:**

-   Maximum quality needed
-   Very small source images
-   Print purposes
-   Large display requirements

**Output:**

-   1000x1000 → 4000x4000
-   File size: ~4-8x larger

## Technical Notes

### AI Model

-   **Source**: nightmareai/real-esrgan
-   **Platform**: Replicate API
-   **Model Type**: Enhanced Super-Resolution GAN
-   **Specialty**: General image upscaling with face-aware option

### Storage

-   Output images stored in Cloudflare R2
-   Automatic key generation with timestamp
-   Prefix: `clarity/YYYY-MM-DD/`
-   Format preserved from input

### Concurrency

-   Uses Replicate limiter to prevent API overload
-   Retry logic with exponential backoff
-   Request queuing during high load

## Best Practices

### Image Input

✅ **DO:**

-   Use decent quality source images
-   Ensure images are not already upscaled
-   Use JPEG for photos, PNG for graphics
-   Keep source images under 10MB
-   Use scale=2 for initial testing

❌ **DON'T:**

-   Upscale extremely low-res images (< 100px)
-   Process already AI-enhanced images
-   Use scale=4 unnecessarily (slower + larger files)
-   Expect magic from severely degraded images

### Performance Optimization

1. **Pre-processing**: Compress large images before upload
2. **Scale Selection**: Use scale=2 unless you specifically need 4x
3. **Face Enhancement**: Only enable when needed
4. **Batch Processing**: Process during off-peak hours

### API Usage

✅ **DO:**

-   Implement retry logic
-   Handle rate limits gracefully
-   Cache results using storage keys
-   Use request_id for debugging
-   Monitor processing times

❌ **DON'T:**

-   Process same image multiple times
-   Ignore error responses
-   Store presigned URLs permanently
-   Skip client-side validation

## Error Codes

| Code                  | HTTP Status | Description                | Solution                            |
| --------------------- | ----------- | -------------------------- | ----------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   | Check file format and scale value   |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry after specified time |
| `INTERNAL_ERROR`      | 500         | Server processing error    | Retry or contact support            |
| `REPLICATE_ERROR`     | 500         | AI model processing failed | Try different image or retry later  |

## Comparison with Other Services

| Feature           | Real-ESRGAN (Clarity) | GFPGAN         | Topaz Labs (Enhance) |
| ----------------- | --------------------- | -------------- | -------------------- |
| General upscaling | ✅ Best               | ⚠️ OK          | ✅ Best              |
| Face enhancement  | ✅ Optional           | ✅ Specialized | ⚠️ General           |
| Scale options     | 2, 4                  | 1, 2, 4        | 2, 4, 6              |
| Speed             | ⚡ Fast               | ⚡ Fast        | ⚠️ Slower            |
| Best for          | All types             | Portraits      | Professional work    |
| Face-specific     | Optional              | Always         | No                   |

## When to Use Which Service

### Use Real-ESRGAN (Clarity)

-   General image upscaling
-   Mixed content (people + scenery)
-   When you want control over face enhancement
-   Fast processing needed
-   Scale 2x or 4x

### Use GFPGAN

-   Face restoration is priority
-   Portrait photos only
-   Need face-specific improvements
-   Old/damaged facial photos

### Use Topaz Labs (Enhance)

-   Professional quality needed
-   Multiple model options
-   Scale 6x needed
-   Non-portrait images

## Troubleshooting

### Output looks over-processed

-   Use scale=2 instead of 4
-   Disable face enhancement
-   Check if input was already processed
-   Ensure input quality is decent

### Faces look unnatural

-   Disable `faceEnhance` parameter
-   Try GFPGAN service instead for better face results
-   Check input image quality

### File size too large

-   Use scale=2 instead of 4
-   Use JPEG format instead of PNG
-   Compress output image after download

### Processing timeout

-   Reduce image size before upload
-   Use scale=2 instead of 4
-   Disable face enhancement
-   Retry during off-peak hours

### 429 Rate Limit

-   Implement exponential backoff
-   Add request queuing
-   Reduce request frequency
-   Contact support for higher limits

## Support

For issues or questions:

1. Check input image format and quality
2. Review error messages and request IDs
3. Verify scale and faceEnhance parameters
4. Test with different images
5. Ensure Replicate API key is configured
6. Verify R2 storage credentials

## Changelog

### v1.0.0 (Current)

-   Real-ESRGAN model integration
-   Scale options: 2, 4
-   Optional face enhancement
-   R2 storage integration
-   Presigned URL generation
-   Rate limiting (60 req/min)
-   Retry logic with exponential backoff
