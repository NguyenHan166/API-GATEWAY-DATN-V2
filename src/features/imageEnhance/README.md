# Image Enhance API

Nâng cấp chất lượng ảnh bằng **Real-ESRGAN** (`nightmareai/real-esrgan`) với lựa chọn tăng cường khuôn mặt.

## Đặc điểm

-   **Model**: `nightmareai/real-esrgan`
-   **Scale**: 2x hoặc 4x
-   **Face enhance**: Tùy chọn `face_enhance`
-   **Pre-scaling**: Resize xuống tối đa 2560px trước khi gọi Replicate

## API Endpoint

```
POST /api/enhance
```

## Request Parameters (form-data)

| Parameter       | Type    | Required | Default        | Description                                        |
| --------------- | ------- | -------- | -------------- | -------------------------------------------------- |
| `image`         | File    | ✅       | -              | File ảnh đầu vào                                   |
| `scale`         | Number  | ❌       | `2`            | Hệ số phóng to: `2` hoặc `4`                       |
| `face_enhance`  | Boolean | ❌       | `false`        | Bật bổ trợ khuôn mặt (alias: `faceEnhance`)        |
| `model`         | String  | ❌       | `real-esrgan`  | Giữ cho kompat cũ, chỉ nhận `real-esrgan`          |

## Ví dụ

### Upscale 2x mặc định

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@photo.jpg"
```

### Upscale 4x + tăng cường khuôn mặt

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@portrait.jpg" \
  -F "scale=4" \
  -F "face_enhance=true"
```

### Gửi từ trình duyệt

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("scale", "2");
formData.append("face_enhance", "true");

const res = await fetch("/api/enhance", { method: "POST", body: formData });
const result = await res.json();
console.log(result.data.url);
```

## Response Format

```json
{
    "success": true,
    "requestId": "abc-123",
    "data": {
        "key": "enhance/real-esrgan/xyz789.jpg",
        "url": "https://pub-xxx.r2.dev/enhance/real-esrgan/xyz789.jpg",
        "presignedUrl": "https://...",
        "expiresIn": 3600,
        "meta": {
            "provider": "nightmareai",
            "model": "real-esrgan",
            "scale": 4,
            "faceEnhance": true,
            "bytes": 5678901,
            "requestId": "abc-123"
        }
    }
}
```

## Lưu ý

1. **Rate Limit**: 60 requests / phút / IP
2. **Processing Time**: ~15-60s (tùy scale và tải Replicate)
3. **Pre-scaling**: Ảnh được resize xuống max 2560px để tối ưu chi phí/thời gian
4. **Model**: [Real-ESRGAN trên Replicate](https://replicate.com/nightmareai/real-esrgan)

## Khuyến nghị

-   Dùng `scale=2` cho hầu hết trường hợp, `scale=4` khi cần in ấn/chi tiết.
-   Bật `face_enhance=true` cho ảnh chân dung hoặc có khuôn mặt chính.
-   Input trên 10MB nên nén lại trước khi gửi để tránh timeout.

## Implementation Details

-   **Retry Logic**: Tự động retry 2 lần nếu API call thất bại
-   **Rate Limiting**: Giới hạn concurrent Replicate jobs
-   **Output Format**: Giữ nguyên format PNG/JPG của input
-   **Storage**: Upload lên R2 với prefix `enhance/real-esrgan`

## Error Handling

-   `Thiếu file 'image'`: Không có file upload
-   `File không phải ảnh`: MIME type không phải image/\*
-   `scale không hợp lệ`: scale không phải 2 hoặc 4
-   `model không hợp lệ`: không phải `real-esrgan`
