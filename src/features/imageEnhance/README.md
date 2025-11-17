# Image Enhance API

Nâng cấp chất lượng ảnh chuyên nghiệp với **Topaz Labs Image Upscale**.

## Đặc điểm

-   **Model**: `topazlabs/image-upscale`
-   **Chất lượng**: Chuyên nghiệp, hàng đầu trong ngành
-   **Scale**: Hỗ trợ 2x, 4x, hoặc 6x
-   **Enhancement Models**: 5 models tối ưu cho các loại ảnh khác nhau
-   **Chi phí**: Tính theo megapixel đầu ra (xem Replicate pricing)

## Enhancement Models

| Model              | Mô tả                            | Sử dụng cho                          |
| ------------------ | -------------------------------- | ------------------------------------ |
| `standard-v2`      | Mục đích chung (mặc định)        | Hầu hết các loại ảnh                 |
| `low-res-v2`       | Tối ưu cho ảnh độ phân giải thấp | Ảnh cũ, ảnh chất lượng thấp          |
| `cgi`              | Tối ưu cho nghệ thuật số         | Digital art, CGI, renders            |
| `high-fidelity-v2` | Bảo toàn chi tiết tốt nhất       | Ảnh phong cảnh, kiến trúc, chân dung |
| `text-refine`      | Tối ưu cho văn bản               | Screenshots, documents, ảnh có chữ   |

## API Endpoint

```
POST /api/enhance
```

## Request Parameters (form-data)

| Parameter | Type   | Required | Default       | Description                        |
| --------- | ------ | -------- | ------------- | ---------------------------------- |
| `image`   | File   | ✅       | -             | File ảnh đầu vào                   |
| `scale`   | Number | ❌       | `2`           | Hệ số phóng to: `2`, `4`, hoặc `6` |
| `model`   | String | ❌       | `standard-v2` | Enhancement model                  |

## Ví dụ

### Cơ bản - Standard V2

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@photo.jpg" \
  -F "scale=2" \
  -F "model=standard-v2"
```

### High Fidelity - Chất lượng cao nhất

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@landscape.jpg" \
  -F "scale=4" \
  -F "model=high-fidelity-v2"
```

### Low Resolution - Cho ảnh cũ

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@old-photo.jpg" \
  -F "scale=6" \
  -F "model=low-res-v2"
```

### CGI - Cho digital art

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@digital-art.jpg" \
  -F "scale=4" \
  -F "model=cgi"
```

### Text Refine - Cho ảnh có văn bản

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@screenshot.png" \
  -F "scale=2" \
  -F "model=text-refine"
```

### Upscale tối đa 6x

```bash
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@small-image.jpg" \
  -F "scale=6" \
  -F "model=standard-v2"
```

## Response Format

```json
{
    "success": true,
    "requestId": "abc-123",
    "data": {
        "key": "enhance/high-fidelity-v2/xyz789.jpg",
        "url": "https://pub-xxx.r2.dev/enhance/high-fidelity-v2/xyz789.jpg",
        "presignedUrl": "https://...",
        "expiresIn": 3600,
        "meta": {
            "provider": "topaz-labs",
            "model": "high-fidelity-v2",
            "scale": 4,
            "bytes": 5678901,
            "requestId": "abc-123"
        }
    }
}
```

## Lưu ý

1. **Rate Limit**: 60 requests / phút / IP
2. **Processing Time**: ~10-30s (tùy scale và model)
3. **Cost**: Tính theo output megapixels
    - Ảnh đầu ra 12MP: $0.05
    - Ảnh đầu ra 24MP: $0.05
    - Ảnh đầu ra 36MP: $0.10
    - Xem đầy đủ pricing tại [Replicate](https://replicate.com/topazlabs/image-upscale)
4. **Pre-scaling**: Ảnh được resize xuống max 4096px trước khi xử lý để tối ưu chi phí

## Khuyến nghị

-   **Standard V2**: Sử dụng cho hầu hết các trường hợp
-   **High Fidelity V2**: Tốt nhất cho:
    -   Ảnh phong cảnh
    -   Ảnh kiến trúc
    -   Chân dung chuyên nghiệp
    -   Khi cần bảo toàn chi tiết tối đa
-   **Low Resolution V2**: Tốt nhất cho:
    -   Ảnh cũ, ảnh scan
    -   Ảnh chất lượng thấp
    -   Ảnh bị nén nhiều
-   **CGI**: Tốt nhất cho:
    -   Digital art
    -   3D renders
    -   Artwork số
    -   Game screenshots
-   **Text Refine**: Tốt nhất cho:
    -   Screenshots
    -   Document scans
    -   Ảnh có nhiều văn bản
    -   Infographics

## So sánh với Improve Clarity (Real-ESRGAN)

| Feature      | Image Enhance (Topaz)         | Improve Clarity (Real-ESRGAN) |
| ------------ | ----------------------------- | ----------------------------- |
| Provider     | Topaz Labs                    | NightmareAI                   |
| Max Scale    | 6x                            | 4x                            |
| Models       | 5 models khác nhau            | 1 model                       |
| Chi phí      | Có phí                        | Miễn phí                      |
| Chất lượng   | Chuyên nghiệp cao             | Tốt                           |
| Tốc độ       | Trung bình                    | Nhanh                         |
| Face Enhance | Không                         | Có                            |
| Best for     | Chất lượng cao, chuyên nghiệp | Sử dụng hàng ngày             |

## Implementation Details

-   **Pre-scaling**: Ảnh được resize xuống max 4096px
-   **Retry Logic**: Tự động retry 2 lần nếu API call thất bại
-   **Rate Limiting**: Giới hạn concurrent Replicate jobs
-   **Output Format**: Giữ nguyên format PNG/JPG của input
-   **Storage**: Upload lên R2 với prefix theo model

## Error Handling

Các lỗi validation:

-   `Thiếu file 'image'`: Không có file upload
-   `File không phải ảnh`: MIME type không phải image/\*
-   `scale không hợp lệ`: scale không phải 2, 4, hoặc 6
-   `model không hợp lệ`: model không tồn tại trong danh sách hỗ trợ
