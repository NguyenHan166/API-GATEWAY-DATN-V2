# Image Quality Features Comparison

Hệ thống cung cấp hai API riêng biệt để nâng cao chất lượng ảnh, mỗi API phù hợp với các nhu cầu khác nhau.

---

## 📊 So Sánh Tổng Quan

| Feature          | Improve Clarity           | Image Enhance                 |
| ---------------- | ------------------------- | ----------------------------- |
| **Endpoint**     | `/api/clarity`            | `/api/enhance`                |
| **Model**        | Real-ESRGAN (NightmareAI) | Topaz Labs Image Upscale      |
| **Chi phí**      | ✅ Miễn phí               | 💰 Có phí (theo megapixels)   |
| **Max Scale**    | 4x                        | 6x                            |
| **Tốc độ**       | ⚡ Nhanh (5-15s)          | 🐢 Trung bình (10-30s)        |
| **Chất lượng**   | ⭐⭐⭐⭐ Tốt              | ⭐⭐⭐⭐⭐ Chuyên nghiệp      |
| **Models**       | 1 model                   | 5 models chuyên biệt          |
| **Face Enhance** | ✅ Có                     | ❌ Không                      |
| **Use Case**     | Hàng ngày, nhanh gọn      | Chuyên nghiệp, chất lượng cao |

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

## 💎 Image Enhance (Topaz Labs)

### Thông tin cơ bản

-   **Endpoint**: `POST /api/enhance`
-   **Model**: `topazlabs/image-upscale`
-   **Chi phí**: Có phí (tính theo output megapixels)
-   **Max input**: 4096px

### Parameters

```bash
image: File (required)
scale: 2 | 4 | 6 (default: 2)
model: string (default: "standard-v2")
```

### Enhancement Models

| Model              | Mô tả                            | Sử dụng cho                      |
| ------------------ | -------------------------------- | -------------------------------- |
| `standard-v2`      | Mục đích chung                   | Hầu hết các loại ảnh             |
| `low-res-v2`       | Tối ưu cho ảnh độ phân giải thấp | Ảnh cũ, ảnh chất lượng thấp      |
| `cgi`              | Tối ưu cho nghệ thuật số         | Digital art, CGI, renders        |
| `high-fidelity-v2` | Bảo toàn chi tiết tốt nhất       | Phong cảnh, kiến trúc, chân dung |
| `text-refine`      | Tối ưu cho văn bản               | Screenshots, documents           |

### Ví dụ sử dụng

```bash
# Standard V2 - General purpose
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@photo.jpg" \
  -F "scale=2" \
  -F "model=standard-v2"

# High Fidelity - Best quality
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@landscape.jpg" \
  -F "scale=4" \
  -F "model=high-fidelity-v2"

# Low Resolution - For old photos
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@old-photo.jpg" \
  -F "scale=6" \
  -F "model=low-res-v2"

# CGI - For digital art
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@digital-art.jpg" \
  -F "scale=4" \
  -F "model=cgi"

# Text Refine - For screenshots
curl -X POST http://localhost:3000/api/enhance \
  -F "image=@screenshot.png" \
  -F "scale=2" \
  -F "model=text-refine"
```

### Khi nào sử dụng?

✅ **Nên dùng khi:**

-   Cần chất lượng chuyên nghiệp cao
-   Ảnh quan trọng (marketing, portfolio, print)
-   Cần upscale lên 6x
-   Có ảnh đặc thù (CGI, low-res, text)
-   Sẵn sàng chi phí cho chất lượng

❌ **Không nên dùng khi:**

-   Ngân sách hạn chế
-   Chỉ cần xử lý nhanh
-   Ảnh không quan trọng
-   Scale 2x-4x là đủ với Real-ESRGAN

---

## 💰 Chi Phí (Topaz Labs)

| Output Megapixels | Units | Giá (USD) |
| ----------------- | ----- | --------- |
| 12 MP             | 1     | $0.05     |
| 24 MP             | 1     | $0.05     |
| 36 MP             | 2     | $0.10     |
| 48 MP             | 2     | $0.10     |
| 60 MP             | 3     | $0.15     |
| 96 MP             | 4     | $0.20     |
| 132 MP            | 5     | $0.24     |
| 168 MP            | 6     | $0.29     |
| 336 MP            | 11    | $0.53     |
| 512 MP            | 17    | $0.82     |

_Note: Topaz Labs sẽ tăng giá từ $0.05 lên $0.08/unit vào 30/11/2025_

---

## 🎬 Use Cases Chi Tiết

### Social Media Posts (Instagram, Facebook)

**Recommend**: Improve Clarity

-   Chi phí: Free ✅
-   Tốc độ: Nhanh
-   Scale 2x là đủ
-   Chất lượng đủ cho web/social

### Professional Photography (Portfolio, Client Work)

**Recommend**: Image Enhance (high-fidelity-v2)

-   Chất lượng tốt nhất
-   Bảo toàn chi tiết
-   Phù hợp cho in ấn
-   Đáng để đầu tư

### E-commerce Product Photos

**Recommend**: Image Enhance (standard-v2)

-   Chất lượng ổn định
-   Không quá đắt
-   Phù hợp cho web

### Portrait Photography

**Option 1**: Improve Clarity (với faceEnhance)

-   Miễn phí
-   Face enhancement tích hợp
-   Đủ cho hầu hết trường hợp

**Option 2**: Image Enhance (high-fidelity-v2)

-   Cho ảnh chân dung cao cấp
-   Print lớn
-   Portfolio chuyên nghiệp

### Old/Vintage Photos

**Recommend**: Image Enhance (low-res-v2)

-   Tối ưu cho ảnh cũ
-   Xử lý ảnh chất lượng thấp tốt
-   Có thể upscale lên 6x

### Digital Art/CGI

**Recommend**: Image Enhance (cgi)

-   Model chuyên biệt
-   Giữ được màu sắc và style
-   Không làm mất chi tiết digital

### Screenshots/Documents

**Recommend**: Image Enhance (text-refine)

-   Tối ưu cho văn bản
-   Giữ chữ sắc nét
-   Không làm mờ text

### Landscape Photography

**Recommend**: Image Enhance (high-fidelity-v2)

-   Bảo toàn chi tiết tốt nhất
-   Tốt cho in lớn
-   Phong cảnh đòi hỏi chi tiết cao

### Everyday Photos (Personal Use)

**Recommend**: Improve Clarity

-   Miễn phí
-   Nhanh
-   Đủ tốt cho lưu trữ cá nhân

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
            "model": "real-esrgan", // hoặc "topaz-labs"
            "scale": 4,
            "faceEnhance": true, // chỉ có trong Improve Clarity
            "provider": "topaz-labs", // chỉ có trong Image Enhance
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

-   **Improve Clarity**: max 2560px prescale
-   **Image Enhance**: max 4096px prescale
-   Cả 2 đều có retry logic (2 lần)
-   Concurrent job limiting via `withReplicateLimiter`

### Storage

-   **Improve Clarity**: prefix `clarity/`
-   **Image Enhance**: prefix `enhance/{model}/`
-   Upload to R2 storage
-   Support cả PNG và JPG

---

## 🚀 Quick Decision Tree

```
Bạn cần upscale ảnh?
│
├─ Ảnh quan trọng (marketing, portfolio)?
│  └─ YES → Image Enhance
│     ├─ Phong cảnh/kiến trúc → high-fidelity-v2
│     ├─ Ảnh cũ → low-res-v2
│     ├─ Digital art → cgi
│     └─ Screenshot/doc → text-refine
│
└─ Ảnh thông thường hoặc giới hạn ngân sách?
   └─ YES → Improve Clarity
      ├─ Có khuôn mặt → faceEnhance=true
      └─ Không có khuôn mặt → faceEnhance=false
```

---

## 📚 Links

-   [Improve Clarity README](./improveClarity/README.md)
-   [Image Enhance README](./imageEnhance/README.md)
-   [Real-ESRGAN on Replicate](https://replicate.com/nightmareai/real-esrgan)
-   [Topaz Labs on Replicate](https://replicate.com/topazlabs/image-upscale)
