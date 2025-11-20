# Real-ESRGAN Image Enhancement API Documentation

## Overview

Sử dụng model `nightmareai/real-esrgan` trên Replicate để upscale ảnh 2x/4x và cải thiện chi tiết, kèm tùy chọn tăng cường khuôn mặt.

## Endpoint

```
POST /api/enhance
```

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter       | Type    | Required | Description                                          | Default       |
| --------------- | ------- | -------- | ---------------------------------------------------- | ------------- |
| `image`         | File    | ✅       | File ảnh (JPEG, PNG, WebP...)                        | -             |
| `scale`         | Number  | ❌       | Hệ số scale: `2` hoặc `4`                            | `2`           |
| `face_enhance`  | Boolean | ❌       | Bật bổ trợ khuôn mặt (alias: `faceEnhance`)          | `false`       |
| `model`         | String  | ❌       | Giữ cho tương thích cũ, chỉ nhận `real-esrgan`       | `real-esrgan` |

### Constraints

-   **Max file size**: 10MB (theo upload middleware)
-   **Supported formats**: JPEG, PNG, WebP…
-   **Pre-scaling**: Resize xuống tối đa 2560px hoặc ~2MP trước khi gọi Replicate

## Response

### Success (200)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "enhance/real-esrgan/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://pub-xxxx.r2.dev/enhance/real-esrgan/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "presigned_url": "https://pub-xxxx.r2.dev/enhance/...?X-Amz-Algorithm=...",
        "expires_in": 3600
    },
    "meta": {
        "provider": "nightmareai",
        "model": "real-esrgan",
        "scale": 2,
        "faceEnhance": false
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### Error (400)

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "Invalid input",
        "code": "VALIDATION_ERROR",
        "details": "scale không hợp lệ. Hỗ trợ: 2, 4"
    }
}
```

### Error (500)

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "Image enhancement failed",
        "code": "INTERNAL_ERROR"
    }
}
```

## Rate Limiting

-   **Limit**: 60 requests / phút / IP
-   **Window**: 60 giây

## Examples

### Basic 2x

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@./photo.jpg"
```

### 4x + Face Enhance

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@./portrait.jpg" \
  -F "scale=4" \
  -F "face_enhance=true"
```

### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "2");
formData.append("faceEnhance", "true");

const response = await fetch("/api/enhance", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Enhanced image:", result.data.url);
```

### Python

```python
import requests

files = {'image': open('photo.jpg', 'rb')}
data = {'scale': '4', 'face_enhance': 'true'}

response = requests.post('http://localhost:3000/api/enhance', files=files, data=data)
print(response.json())
```

## Processing Details

1. Resize ảnh về tối đa 2560px (tỷ lệ gốc) để tối ưu chi phí/thời gian.
2. Gửi buffer trực tiếp lên Replicate model `nightmareai/real-esrgan`.
3. Áp dụng scale 2x/4x và tùy chọn `face_enhance`.
4. Upload output lên R2 với prefix `enhance/real-esrgan`.
5. Trả về key, URL public và presigned URL.

**Processing time**: ~15-60s tùy scale/kích thước input và tải Replicate.

## Best Practices

-   Dùng `scale=2` cho đa số trường hợp; `scale=4` khi cần in ấn/ảnh lớn.
-   Bật `face_enhance` cho chân dung hoặc ảnh có khuôn mặt chính.
-   Tránh input quá lớn hoặc ảnh đã xử lý AI nhiều lần (dễ sinh artifact).
-   Giữ bản gốc để so sánh vì Real-ESRGAN thiên về sharpness.

## Error Codes

| Code                  | HTTP Status | Description                        | Solution                                 |
| --------------------- | ----------- | ---------------------------------- | ---------------------------------------- |
| `VALIDATION_ERROR`    | 400         | Tham số không hợp lệ               | Kiểm tra scale (2,4) và model            |
| `RATE_LIMIT_EXCEEDED` | 429         | Quá giới hạn                       | Chờ 60s và thử lại                       |
| `INTERNAL_ERROR`      | 500         | Lỗi xử lý nội bộ                   | Retry hoặc liên hệ hỗ trợ                |
| `REPLICATE_ERROR`     | 500         | Lỗi từ Replicate/Model             | Thử ảnh khác hoặc giảm scale             |

## Changelog

### v2.0.0 (Current)

-   Chuyển sang model `nightmareai/real-esrgan`
-   Bổ sung tùy chọn `face_enhance`
-   Giới hạn scale: 2x/4x, pre-scale 2560px
-   Prefix R2: `enhance/real-esrgan`
