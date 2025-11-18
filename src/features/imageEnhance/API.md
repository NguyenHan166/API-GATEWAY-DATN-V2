# Topaz Labs Image Enhancement API Documentation

## Overview

Topaz Labs cung cấp AI models chuyên nghiệp cho image enhancement với nhiều mô hình khác nhau tối ưu cho từng loại ảnh. Service này sử dụng model từ Topaz Labs để:

-   Tăng độ phân giải ảnh với chất lượng cao
-   Cải thiện chi tiết và độ sắc nét
-   Khử nhiễu thông minh
-   Hỗ trợ nhiều loại ảnh (standard, low-res, CGI, text)

## Endpoint

```
POST /api/enhance
```

## Description

Upload ảnh và nhận về ảnh đã được enhance bằng Topaz Labs AI models với lựa chọn model phù hợp cho từng loại ảnh.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter | Type   | Required | Description                       | Default         |
| --------- | ------ | -------- | --------------------------------- | --------------- |
| `image`   | File   | ✅       | File ảnh (JPEG, PNG, WebP, etc.)  | -               |
| `scale`   | Number | ❌       | Hệ số scale (2, 4, hoặc 6)        | `2`             |
| `model`   | String | ❌       | Enhancement model (xem bảng dưới) | `"standard-v2"` |

### Constraints

-   **Max file size**: 10MB (theo config upload middleware)
-   **Supported formats**: JPEG, PNG, WebP và hầu hết các format ảnh
-   **Scale options**: `2`, `4`, `6`

## Enhancement Models

| Model              | Description                         | Best For                          |
| ------------------ | ----------------------------------- | --------------------------------- |
| `standard-v2`      | General purpose enhancement         | Most photos, balanced quality     |
| `low-res-v2`       | Optimized for low-resolution images | Very small/pixelated images       |
| `cgi`              | Designed for digital art and CGI    | Digital art, 3D renders, game art |
| `high-fidelity-v2` | Preserves maximum detail            | High-quality photos, print work   |
| `text-refine`      | Optimized for text clarity          | Screenshots, documents with text  |

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "enhance/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://pub-xxxx.r2.dev/enhance/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "presigned_url": "https://pub-xxxx.r2.dev/enhance/...?X-Amz-Algorithm=...",
        "expires_in": 3600
    },
    "meta": {
        "model": "topaz-labs/standard-v2",
        "scale": 2,
        "input_size": {
            "width": 1920,
            "height": 1080
        },
        "output_size": {
            "width": 3840,
            "height": 2160
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
        "details": "scale không hợp lệ. Hỗ trợ: 2, 4, 6"
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
        "message": "Image enhancement failed",
        "code": "INTERNAL_ERROR"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

## Response Fields

| Field                | Type   | Description                        |
| -------------------- | ------ | ---------------------------------- |
| `request_id`         | String | Unique request identifier          |
| `status`             | String | "success" or "error"               |
| `data.key`           | String | R2 storage key                     |
| `data.url`           | String | Public URL (if available)          |
| `data.presigned_url` | String | Presigned URL for secure access    |
| `data.expires_in`    | Number | Presigned URL expiration (seconds) |
| `meta.model`         | String | AI model used                      |
| `meta.scale`         | Number | Scale factor applied               |
| `meta.input_size`    | Object | Original image dimensions          |
| `meta.output_size`   | Object | Enhanced image dimensions          |

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

### Basic Usage (Standard Model, 2x Scale)

#### cURL

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@./photo.jpg"
```

#### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);

const response = await fetch("http://localhost:3000/api/enhance", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Enhanced image:", result.data.url);
console.log("Model used:", result.meta.model);
```

#### Python

```python
import requests

files = {'image': open('photo.jpg', 'rb')}

response = requests.post('http://localhost:3000/api/enhance', files=files)
result = response.json()

print(f"Enhanced: {result['data']['url']}")
```

### Low-Res Image Enhancement (6x Scale)

#### cURL

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@./pixelated.jpg" \
  -F "scale=6" \
  -F "model=low-res-v2"
```

#### JavaScript

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "6");
formData.append("model", "low-res-v2");

const response = await fetch("http://localhost:3000/api/enhance", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("6x enhanced:", result.data.url);
```

#### Python

```python
files = {'image': open('pixelated.jpg', 'rb')}
data = {
    'scale': '6',
    'model': 'low-res-v2'
}

response = requests.post(
    'http://localhost:3000/api/enhance',
    files=files,
    data=data
)
```

### Digital Art Enhancement

#### cURL

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@./artwork.png" \
  -F "scale=4" \
  -F "model=cgi"
```

#### JavaScript

```javascript
const formData = new FormData();
formData.append("image", artworkFile);
formData.append("scale", "4");
formData.append("model", "cgi");

const response = await fetch("http://localhost:3000/api/enhance", {
    method: "POST",
    body: formData,
});

const result = await response.json();
```

### Text/Screenshot Enhancement

#### cURL

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@./screenshot.png" \
  -F "scale=2" \
  -F "model=text-refine"
```

## Model Selection Guide

### Standard V2 (Default)

**Best for:**

-   General photos
-   Balanced quality
-   When unsure which model to use
-   Mixed content

**Characteristics:**

-   ✅ Versatile
-   ✅ Good for most use cases
-   ✅ Balanced speed and quality

### Low-Res V2

**Best for:**

-   Extremely pixelated images
-   Old scanned photos
-   Very small images (< 512px)
-   Heavy upscaling needed

**Characteristics:**

-   ✅ Excellent for 6x upscaling
-   ✅ Handles severe degradation
-   ⚠️ May over-smooth details on high-quality inputs

### CGI

**Best for:**

-   Digital artwork
-   3D renders
-   Video game screenshots
-   Anime/cartoon images
-   Illustrations

**Characteristics:**

-   ✅ Preserves sharp edges
-   ✅ Great for synthetic images
-   ⚠️ Not ideal for photos

### High-Fidelity V2

**Best for:**

-   Professional photography
-   Print work
-   Maximum detail preservation
-   High-quality source images

**Characteristics:**

-   ✅ Maximum detail retention
-   ✅ Professional results
-   ⚠️ Slower processing
-   ⚠️ Requires good source quality

### Text Refine

**Best for:**

-   Screenshots with text
-   Documents
-   UI elements
-   Diagrams with text

**Characteristics:**

-   ✅ Sharpens text significantly
-   ✅ Preserves readability
-   ⚠️ Not for general photos

## Scale Selection Guide

### Scale 2

**Output:** 1000x1000 → 2000x2000
**Best for:**

-   General enhancement
-   Moderate upscaling
-   Faster processing
-   Balanced file size

### Scale 4

**Output:** 1000x1000 → 4000x4000
**Best for:**

-   Significant upscaling
-   Print preparation
-   Large displays
-   Professional work

### Scale 6

**Output:** 1000x1000 → 6000x6000
**Best for:**

-   Extreme upscaling
-   Very small source images
-   Maximum quality
-   Large format printing

**Warning:** Scale 6 produces very large files and takes longer to process!

## Processing Details

### How Topaz Labs Works

1. **Analysis**: AI analyzes image content and structure
2. **Model Selection**: Uses specified model for enhancement
3. **Detail Generation**: Generates realistic high-res details
4. **Noise Reduction**: Removes artifacts and noise
5. **Upscaling**: Scales to target resolution
6. **Post-processing**: Refines final output

### Processing Time

-   **Scale 2**: 30-60 seconds
-   **Scale 4**: 60-120 seconds
-   **Scale 6**: 120-180 seconds

_Time varies by model, image complexity, and Replicate API load_

## Use Cases

### Photography

-   Upscale for large prints
-   Enhance old photos
-   Prepare for exhibitions
-   Improve photo quality

### Digital Art

-   Upscale artwork for prints
-   Enhance game assets
-   Improve illustrations
-   Prepare for high-res displays

### E-commerce

-   Product photography enhancement
-   Catalog image upscaling
-   Detail improvement
-   Zoom-ready images

### Professional Work

-   Client deliverables
-   Print preparation
-   Large format displays
-   Portfolio enhancement

### Documents & UI

-   Screenshot enhancement
-   UI mockup upscaling
-   Document scanning
-   Presentation images

## Best Practices

### Model Selection

✅ **DO:**

-   Use `standard-v2` as default
-   Use `low-res-v2` for pixelated images
-   Use `cgi` for digital art
-   Use `text-refine` for screenshots/documents
-   Test different models if unsure

❌ **DON'T:**

-   Use `cgi` for photos
-   Use `text-refine` for photos
-   Use `high-fidelity-v2` on low-quality inputs

### Scale Selection

✅ **DO:**

-   Start with scale 2 for testing
-   Use scale 6 only when necessary
-   Consider file size implications
-   Match scale to output needs

❌ **DON'T:**

-   Always use scale 6 (slow + large files)
-   Upscale already high-res images unnecessarily
-   Ignore processing time for large scales

### Image Input

✅ **DO:**

-   Use decent quality source images
-   Choose appropriate model for content type
-   Compress images before upload if > 10MB
-   Keep originals for comparison

❌ **DON'T:**

-   Expect miracles from extremely degraded images
-   Process already AI-enhanced images
-   Use wrong model for content type

## Technical Notes

### AI Models

-   **Source**: Topaz Labs
-   **Platform**: Replicate API
-   **Model Type**: Multiple specialized enhancement models
-   **Specialty**: Professional-grade image enhancement

### Storage

-   Output images stored in Cloudflare R2
-   Automatic key generation with timestamp
-   Prefix: `enhance/YYYY-MM-DD/`
-   Format preserved from input

### Concurrency

-   Uses Replicate limiter to prevent API overload
-   Retry logic with exponential backoff
-   Request queuing during high load

## Error Codes

| Code                  | HTTP Status | Description                | Solution                             |
| --------------------- | ----------- | -------------------------- | ------------------------------------ |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   | Check scale (2,4,6) and model values |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry after specified time  |
| `INTERNAL_ERROR`      | 500         | Server processing error    | Retry or contact support             |
| `REPLICATE_ERROR`     | 500         | AI model processing failed | Try different image or model         |

## Comparison with Other Services

| Feature           | Topaz Labs (Enhance) | Real-ESRGAN (Clarity) | GFPGAN         |
| ----------------- | -------------------- | --------------------- | -------------- |
| Scale options     | 2, 4, 6              | 2, 4                  | 1, 2, 4        |
| Model variety     | ✅ 5 models          | ❌ 1 model            | ❌ 1 model     |
| Max upscaling     | ✅ 6x                | 4x                    | 4x             |
| General upscaling | ✅ Best              | ✅ Good               | ⚠️ OK          |
| Face-specific     | ⚠️ General           | Optional              | ✅ Specialized |
| Digital art       | ✅ CGI model         | ⚠️ OK                 | ❌ No          |
| Text enhancement  | ✅ Text model        | ⚠️ OK                 | ❌ No          |
| Processing speed  | ⚠️ Slower            | ⚡ Fast               | ⚡ Fast        |
| Best for          | Professional work    | General use           | Portraits      |

## When to Use Which Service

### Use Topaz Labs (Enhance)

-   Professional quality needed
-   Have specific content type (CGI, text, etc.)
-   Need 6x upscaling
-   Want maximum detail preservation
-   Working with digital art

### Use Real-ESRGAN (Clarity)

-   General upscaling
-   Faster processing needed
-   Optional face enhancement
-   Standard photos

### Use GFPGAN

-   Portrait photos only
-   Face restoration priority
-   Old/damaged facial photos

## Troubleshooting

### Wrong model chosen

-   Review model selection guide
-   Test with `standard-v2` first
-   Match model to content type

### Output quality not improved

-   Check if input is already high quality
-   Try different model
-   Ensure source isn't already AI-enhanced
-   Verify image isn't too degraded

### File size too large (scale 6)

-   Use scale 4 instead
-   Compress output after processing
-   Use JPEG instead of PNG
-   Consider if 6x is necessary

### Processing timeout

-   Reduce scale (6→4→2)
-   Use faster model (`standard-v2`)
-   Reduce input image size
-   Retry during off-peak hours

### Model doesn't match content

-   CGI model on photos → Use `standard-v2`
-   Text model on artwork → Use `cgi` or `standard-v2`
-   Standard on very low-res → Use `low-res-v2`

## Support

For issues or questions:

1. Review model selection guide
2. Check error messages and request IDs
3. Test with different models and scales
4. Verify input image quality
5. Ensure Replicate API key is configured
6. Check R2 storage credentials

## Changelog

### v1.0.0 (Current)

-   Topaz Labs models integration
-   5 specialized enhancement models
-   Scale options: 2, 4, 6
-   R2 storage integration
-   Presigned URL generation
-   Rate limiting (60 req/min)
-   Retry logic with exponential backoff
