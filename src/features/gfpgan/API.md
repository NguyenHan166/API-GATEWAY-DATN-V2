# GFPGAN Face Restoration API Documentation

## Overview

GFPGAN (Generative Facial Prior GAN) là AI model chuyên về khôi phục chất lượng khuôn mặt trong ảnh. Service này sử dụng model từ TencentARC để:

-   Khôi phục chi tiết khuôn mặt bị mờ/nhiễu
-   Tăng độ phân giải ảnh chân dung
-   Loại bỏ các artifact do nén ảnh
-   Cải thiện độ sắc nét của các đặc điểm khuôn mặt

## Endpoint

```
POST /api/upscale
```

## Description

Upload ảnh chân dung và nhận về ảnh đã được khôi phục/tăng chất lượng bằng AI model GFPGAN.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter | Type   | Required | Description                          | Default  |
| --------- | ------ | -------- | ------------------------------------ | -------- |
| `image`   | File   | ✅       | File ảnh (JPEG, PNG, WebP)           | -        |
| `scale`   | Number | ❌       | Hệ số scale (1, 2, hoặc 4)           | `2`      |
| `version` | String | ❌       | Phiên bản model ("v1.3" hoặc "v1.4") | `"v1.4"` |

### Constraints

-   **Max file size**: 10MB (theo config upload middleware)
-   **Supported formats**: JPEG, PNG, WebP
-   **Recommended**: Ảnh chân dung có khuôn mặt rõ ràng
-   **Scale options**:
    -   `1`: Giữ nguyên kích thước, chỉ khôi phục chất lượng
    -   `2`: Tăng 2x kích thước (recommended)
    -   `4`: Tăng 4x kích thước

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "gfpgan/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://pub-xxxx.r2.dev/gfpgan/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "presigned_url": "https://pub-xxxx.r2.dev/gfpgan/...?X-Amz-Algorithm=...",
        "expires_in": 3600
    },
    "meta": {
        "model": "tencentarc/gfpgan",
        "version": "v1.4",
        "scale": 2,
        "input_size": {
            "width": 512,
            "height": 768
        },
        "output_size": {
            "width": 1024,
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
        "message": "GFPGAN processing failed",
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
| `meta.version`       | String | Model version                      |
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

### cURL

```bash
# Basic usage (scale 2x, version v1.4)
curl -X POST http://localhost:3000/api/upscale \
  -F "image=@./portrait.jpg"

# With custom scale and version
curl -X POST http://localhost:3000/api/upscale \
  -F "image=@./old_photo.jpg" \
  -F "scale=4" \
  -F "version=v1.4"
```

### JavaScript (Fetch API)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "2");
formData.append("version", "v1.4");

const response = await fetch("http://localhost:3000/api/upscale", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Enhanced image URL:", result.data.url);
console.log("Output size:", result.meta.output_size);
```

### Python (requests)

```python
import requests

url = "http://localhost:3000/api/upscale"
files = {'image': open('portrait.jpg', 'rb')}
data = {
    'scale': '2',
    'version': 'v1.4'
}

response = requests.post(url, files=files, data=data)
result = response.json()

print(f"Enhanced image: {result['data']['url']}")
print(f"Scale: {result['meta']['scale']}x")
```

### Node.js (FormData)

```javascript
import fs from "fs";
import FormData from "form-data";
import fetch from "node-fetch";

const form = new FormData();
form.append("image", fs.createReadStream("./portrait.jpg"));
form.append("scale", "2");
form.append("version", "v1.4");

const response = await fetch("http://localhost:3000/api/upscale", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(result);
```

## Model Versions

### v1.4 (Recommended)

-   Latest version with improved quality
-   Better handling of complex facial features
-   Reduced artifacts
-   More natural skin texture

### v1.3

-   Previous stable version
-   Good for general face restoration
-   Slightly faster processing

## Processing Details

### How GFPGAN Works

1. **Face Detection**: Automatically detects faces in the image
2. **Feature Extraction**: Analyzes facial features and details
3. **GAN Restoration**: Uses generative AI to reconstruct high-quality details
4. **Upscaling**: Scales the image to desired resolution
5. **Post-processing**: Blends and refines the final output

### Processing Time

-   **Small images** (< 1MP): 15-30 seconds
-   **Medium images** (1-4MP): 30-45 seconds
-   **Large images** (> 4MP): 45-90 seconds

_Note: Time depends on Replicate API load and image complexity_

## Use Cases

### Photo Restoration

-   Khôi phục ảnh cũ bị mờ/hư hỏng
-   Sửa ảnh bị nhiễu do scan
-   Cải thiện ảnh chất lượng thấp

### Profile Pictures

-   Tăng chất lượng ảnh đại diện
-   Làm sắc nét ảnh selfie
-   Chuẩn bị ảnh cho mạng xã hội

### Professional Photos

-   Enhance ảnh chân dung chuyên nghiệp
-   Sửa ảnh bị out of focus
-   Cải thiện ảnh event/wedding

### E-commerce

-   Tăng chất lượng ảnh sản phẩm có người
-   Cải thiện ảnh model
-   Làm đẹp ảnh lifestyle

## Technical Notes

### AI Model

-   **Source**: TencentARC/GFPGAN
-   **Platform**: Replicate API
-   **Model Type**: Generative Adversarial Network (GAN)
-   **Specialty**: Blind face restoration

### Storage

-   Output images stored in Cloudflare R2
-   Automatic key generation with timestamp
-   Prefix: `gfpgan/YYYY-MM-DD/`
-   Format preserved from input (JPEG/PNG/WebP)

### Concurrency

-   Uses Replicate limiter to prevent API overload
-   Retry logic with exponential backoff
-   Request queuing during high load

## Best Practices

### Image Quality

✅ **DO:**

-   Use images with clearly visible faces
-   Ensure decent lighting in original photo
-   Center the face in the frame
-   Use JPEG/PNG format
-   Upload at least 256x256px resolution

❌ **DON'T:**

-   Upload extremely low-res images (< 128px)
-   Use images with multiple faces (focus may split)
-   Expect miracles from extremely damaged photos
-   Process already high-quality modern photos (minimal improvement)

### Scale Selection

-   **Scale 1**: For quality restoration without size increase
-   **Scale 2**: Balanced quality and file size (recommended)
-   **Scale 4**: Maximum quality, larger file size

### API Usage

✅ **DO:**

-   Implement retry logic with exponential backoff
-   Handle rate limits gracefully
-   Cache results using the `key` field
-   Use `request_id` for debugging
-   Compress images before upload if very large

❌ **DON'T:**

-   Process the same image multiple times
-   Ignore error responses
-   Store presigned URLs long-term (they expire!)
-   Skip validation on client side

## Error Codes

| Code                  | HTTP Status | Description                | Solution                            |
| --------------------- | ----------- | -------------------------- | ----------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   | Check file format, scale, version   |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry after specified time |
| `INTERNAL_ERROR`      | 500         | Server processing error    | Retry or contact support            |
| `REPLICATE_ERROR`     | 500         | AI model processing failed | Try different image or retry later  |

## Comparison with Other Services

| Feature           | GFPGAN  | Real-ESRGAN (Clarity) | Topaz Labs (Enhance) |
| ----------------- | ------- | --------------------- | -------------------- |
| Face restoration  | ✅ Best | ✅ Good               | ⚠️ General           |
| Scale options     | 1,2,4   | 2,4                   | 2,4,6                |
| Face-specific     | ✅ Yes  | Optional              | No                   |
| General upscaling | ⚠️ OK   | ✅ Better             | ✅ Best              |
| Best for          | Faces   | Mixed content         | All types            |

## Troubleshooting

### Face not enhanced properly

-   Try different version (v1.3 vs v1.4)
-   Ensure face is clearly visible
-   Check input image quality
-   Try scale=4 for better results

### Output looks unnatural

-   Reduce scale to 2 or 1
-   Try different model version
-   Check if input image was already processed

### Processing timeout

-   Reduce image size before upload
-   Use scale=2 instead of 4
-   Retry during off-peak hours

### 429 Rate Limit Error

-   Implement client-side rate limiting
-   Add queue system for batch processing
-   Wait for retry window to expire

## Support

For issues or questions:

1. Check input image quality and format
2. Review error messages and request IDs
3. Verify scale and version parameters
4. Ensure Replicate API key is configured
5. Check R2 storage credentials

## Changelog

### v1.0.0 (Current)

-   GFPGAN v1.3 and v1.4 support
-   Scale options: 1, 2, 4
-   R2 storage integration
-   Presigned URL generation
-   Rate limiting (60 req/min)
-   Retry logic with exponential backoff
