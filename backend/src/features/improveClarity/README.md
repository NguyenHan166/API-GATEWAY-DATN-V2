# Improve Clarity API

Nâng cao độ rõ nét và chất lượng ảnh với **Real-ESRGAN**.

## Đặc điểm

-   **Model**: `nightmareai/real-esrgan`
-   **Chất lượng**: Tốt, phù hợp cho hầu hết các trường hợp
-   **Scale**: Hỗ trợ 2x hoặc 4x
-   **Face Enhance**: Có thể bật/tắt tính năng cải thiện khuôn mặt
-   **Chi phí**: Miễn phí
-   **Auto-resize**: Ảnh > ~2MP tự động resize để tránh lỗi GPU memory

## API Endpoint

```
POST /api/clarity
```

## Request Parameters (form-data)

| Parameter     | Type    | Required | Default | Description                  |
| ------------- | ------- | -------- | ------- | ---------------------------- |
| `image`       | File    | ✅       | -       | File ảnh đầu vào             |
| `scale`       | Number  | ❌       | `2`     | Hệ số phóng to: `2` hoặc `4` |
| `faceEnhance` | Boolean | ❌       | `false` | Cải thiện khuôn mặt          |

## Ví dụ

### Cơ bản - Scale 2x

```bash
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@photo.jpg" \
  -F "scale=2"
```

### Scale 4x

```bash
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@photo.jpg" \
  -F "scale=4"
```

### Với Face Enhancement

```bash
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@portrait.jpg" \
  -F "scale=4" \
  -F "faceEnhance=true"
```

## Response Format

```json
{
    "success": true,
    "requestId": "abc-123",
    "data": {
        "key": "clarity/xyz789.jpg",
        "url": "https://pub-xxx.r2.dev/clarity/xyz789.jpg",
        "presignedUrl": "https://...",
        "expiresIn": 3600,
        "meta": {
            "model": "real-esrgan",
            "scale": 4,
            "faceEnhance": true,
            "bytes": 2456789,
            "requestId": "abc-123"
        }
    }
}
```

## Lưu ý

1. **Rate Limit**: 60 requests / phút / IP
2. **Processing Time**: ~5-15s
3. **Cost**: Miễn phí
4. **Auto-resize**: Ảnh > ~2MP (~1414x1414) tự động resize xuống để tránh lỗi GPU memory
5. **Face Enhance**: Bật khi ảnh có khuôn mặt bị mờ hoặc không rõ

## Khuyến nghị

-   Sử dụng **scale=2**: Cho hầu hết các trường hợp
-   Sử dụng **scale=4**: Khi cần phóng to nhiều hơn
-   Bật **faceEnhance=true**: Khi:
    -   Ảnh có khuôn mặt người
    -   Khuôn mặt bị mờ hoặc thiếu chi tiết
    -   Ảnh chân dung hoặc ảnh nhóm

## So sánh với Image Enhance (Topaz Labs)

| Feature      | Improve Clarity (Real-ESRGAN) | Image Enhance (Topaz)         |
| ------------ | ----------------------------- | ----------------------------- |
| Provider     | NightmareAI                   | Topaz Labs                    |
| Max Scale    | 4x                            | 6x                            |
| Models       | 1 model                       | 5 models khác nhau            |
| Chi phí      | Miễn phí                      | Có phí                        |
| Chất lượng   | Tốt                           | Chuyên nghiệp cao             |
| Tốc độ       | Nhanh                         | Trung bình                    |
| Face Enhance | Có                            | Không                         |
| Best for     | Sử dụng hàng ngày             | Chất lượng cao, chuyên nghiệp |

**Khi nào dùng Improve Clarity?**

-   Sử dụng hàng ngày
-   Cần xử lý nhanh
-   Không muốn chi phí
-   Ảnh có khuôn mặt cần cải thiện

**Khi nào dùng Image Enhance?**

-   Cần chất lượng chuyên nghiệp
-   Ảnh quan trọng (marketing, portfolio, etc.)
-   Cần upscale lên 6x
-   Có ảnh đặc thủ (CGI, low-res, text, etc.)

## Implementation Details

-   **Auto-resize**: Ảnh > ~2MP tự động resize giữ nguyên aspect ratio
-   **GPU Limit**: Max ~2,000,000 pixels (~1414x1414 or 2000x1000)
-   **Retry Logic**: Tự động retry 2 lần nếu API call thất bại
-   **Rate Limiting**: Giới hạn concurrent Replicate jobs
-   **Output Format**: Giữ nguyên format PNG/JPG của input
-   **Storage**: Upload lên R2 với prefix `clarity`

## Error Handling

Các lỗi validation:

-   `Thiếu file 'image'`: Không có file upload
-   `File không phải ảnh`: MIME type không phải image/\*
-   `scale không hợp lệ`: scale không phải 2 hoặc 4
