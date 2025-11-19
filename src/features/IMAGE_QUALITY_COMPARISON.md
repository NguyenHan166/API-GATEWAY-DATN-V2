# Image Quality Features Comparison

Hệ thống cung cấp hai API riêng biệt để nâng cao chất lượng ảnh, mỗi API phù hợp với các nhu cầu khác nhau.

---

## 📊 So Sánh Tổng Quan

| Feature          | Improve Clarity                     | Image Enhance                         |
| ---------------- | ----------------------------------- | ------------------------------------- |
| **Endpoint**     | `/api/clarity`                      | `/api/enhance`                        |
| **Model**        | Real-ESRGAN (NightmareAI)           | Real-ESRGAN (NightmareAI)             |
| **Chi phí**      | Theo compute Replicate (thấp)       | Theo compute Replicate (thấp)         |
| **Max Scale**    | 4x                                  | 4x                                    |
| **Tốc độ**       | ⚡ Nhanh (15-45s)                    | ⚡ Nhanh (15-60s)                      |
| **Chất lượng**   | ⭐⭐⭐⭐ Tối ưu độ rõ              | ⭐⭐⭐⭐ Tối ưu sắc nét + URL công khai |
| **Models**       | 1 (real-esrgan)                     | 1 (real-esrgan)                       |
| **Face Enhance** | ✅ Có (tùy chọn)                    | ✅ Có (tùy chọn)                      |
| **Use Case**     | Cải thiện nhanh, ít cấu hình        | Upscale + chia sẻ qua R2 URL          |

---

## 🎯 Improve Clarity (Real-ESRGAN)

### Thông tin cơ bản

-   **Endpoint**: `POST /api/clarity`
-   **Model**: `nightmareai/real-esrgan`
-   **Chi phí**: Miễn phí
-   **Max input**: 2560px (khuyến nghị 1440p)

### Parameters

```bash
image: File (required)
scale: 2 | 4 (default: 2)
faceEnhance: boolean (default: false)
```

### Ví dụ sử dụng

```bash
# Basic
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@photo.jpg" \
  -F "scale=2"

# With face enhancement
curl -X POST http://localhost:3000/api/clarity \
  -F "image=@portrait.jpg" \
  -F "scale=4" \
  -F "faceEnhance=true"
```

### Khi nào sử dụng?

✅ **Nên dùng khi:**

-   Cần xử lý nhanh
-   Không muốn chi phí
-   Ảnh hàng ngày, không quan trọng lắm
-   Ảnh có khuôn mặt cần cải thiện
-   Upscale 2x-4x là đủ

❌ **Không nên dùng khi:**

-   Cần chất lượng tuyệt đối
-   Ảnh quan trọng (marketing, portfolio)
-   Cần upscale lên 6x
-   Cần tối ưu cho loại ảnh đặc biệt (CGI, text, etc.)

---

## 💎 Image Enhance (Real-ESRGAN)

### Thông tin cơ bản

-   **Endpoint**: `POST /api/enhance`
-   **Model**: `nightmareai/real-esrgan`
-   **Chi phí**: Theo compute Replicate (tương tự Improve Clarity)
-   **Max input**: 2560px (pre-scale trước khi gọi model)

### Parameters

```bash
image: File (required)
scale: 2 | 4 (default: 2)
face_enhance: boolean (default: false)
model: string (default: "real-esrgan")
```

### Ví dụ sử dụng

```bash
# Basic 2x
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@photo.jpg"

# 4x + Face enhance
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@portrait.jpg" \
  -F "scale=4" \
  -F "face_enhance=true"
```

### Khi nào sử dụng?

✅ **Nên dùng khi:**

-   Cần kết quả upscale và URL/presigned từ R2
-   Muốn bật `face_enhance` cho chân dung
-   Cần 2x/4x nhanh, sắc nét
-   Cần endpoint /api/enhance cho compatibility cũ

❌ **Không nên dùng khi:**

-   Muốn giữ nguyên kích thước >2560px (sẽ bị pre-scale)
-   Ảnh đã bị sharpen quá nhiều (có thể tạo artifact)

---

## 💰 Chi Phí

-   Cả hai endpoint dùng chung Real-ESRGAN trên Replicate, chi phí phụ thuộc thời gian chạy (thấp, tương đương nhau).
-   Không còn pricing theo megapixel như Topaz Labs; không cần chọn model phụ.

---

## 🎬 Use Cases Chi Tiết

### Social Media Posts (Instagram, Facebook)

**Recommend**: Improve Clarity (scale 2x)

-   Nhanh, nhẹ, đủ tốt cho web/social
-   Face enhance tùy chọn nếu có chân dung

### Professional Photography / Marketing

**Recommend**: Image Enhance (scale 4x, face_enhance khi cần)

-   Trả về URL/presigned R2 sẵn dùng
-   Thích hợp cho in ấn/portfolio

### E-commerce Product Photos

**Recommend**: Image Enhance (scale 2x hoặc 4x)

-   Upscale và lấy URL public ngay
-   Giữ chi tiết tốt, giảm noise

### Portrait Photography

-   **Nhanh**: Improve Clarity với `faceEnhance=true`
-   **Hoàn thiện**: Image Enhance với `face_enhance=true` để lấy link chia sẻ

### Old/Vintage Photos

**Recommend**: Image Enhance (scale 4x, `face_enhance=true` nếu có người)

-   Làm nét + phóng to cùng lúc
-   Pre-scale giúp giảm artifact khi ảnh quá lớn

### Digital Art / Screenshots / Documents

**Recommend**: Image Enhance (scale 2x, `face_enhance=false`)

-   Giữ viền và text sắc nét
-   Không cần chọn model riêng lẻ

### Everyday Photos (Personal Use)

**Recommend**: Improve Clarity (scale 2x)

-   Đơn giản, ít tham số
-   Lưu trữ nhanh cho ảnh cá nhân

---

## 📝 Response Format (Cả 2 APIs)

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
            "model": "nightmareai/real-esrgan",
            "scale": 4,
            "faceEnhance": true,
            "provider": "nightmareai",
            "bytes": 2456789,
            "requestId": "abc-123"
        }
    }
}
```

---

## ⚙️ Technical Details

### Rate Limiting

-   **Cả 2 APIs**: 60 requests / phút / IP
-   Sử dụng `rateLimitPerRoute` middleware

### Processing

-   Cả hai endpoint prescale ảnh về tối đa 2560px trước khi gửi model
-   Retry logic (2 lần) và limiter `withReplicateLimiter`

### Storage

-   **Improve Clarity**: prefix `clarity/`
-   **Image Enhance**: prefix `enhance/real-esrgan/`
-   Upload lên R2, hỗ trợ PNG/JPG

---

## 🚀 Quick Decision Tree

```
Bạn cần upscale ảnh?
│
├─ Muốn lấy URL/presigned để chia sẻ?
│  └─ YES → Image Enhance (/api/enhance)
│       ├─ Ảnh có mặt → face_enhance=true
│       └─ Ảnh thường → face_enhance=false
│
└─ Cần xử lý nhanh, ít cấu hình?
   └─ YES → Improve Clarity (/api/clarity)
        ├─ Ảnh có mặt → faceEnhance=true
        └─ Ảnh thường → faceEnhance=false
```

---

## 📚 Links

-   [Improve Clarity README](./improveClarity/README.md)
-   [Image Enhance README](./imageEnhance/README.md)
-   [Real-ESRGAN on Replicate](https://replicate.com/nightmareai/real-esrgan)
