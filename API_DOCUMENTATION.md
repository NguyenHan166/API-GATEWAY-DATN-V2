# API Gateway - RESTful API Documentation

> **Base URL**: `http://localhost:3000/api`  
> **Version**: v1.0.0  
> **Last Updated**: November 18, 2025

## 📋 Table of Contents

-   [Rate Limiting](#rate-limiting)
-   [Response Format](#response-format)
-   [Error Handling](#error-handling)
-   [API Endpoints](#api-endpoints)
    -   [Manifest Management](#1-manifest-management)
    -   [Image Upscaling (GFPGAN)](#2-image-upscaling-gfpgan)
    -   [Portrait Relighting (IC-Light)](#3-portrait-relighting-ic-light)
    -   [Clarity Improvement (Real-ESRGAN)](#4-clarity-improvement-real-esrgan)
    -   [Image Enhancement (Real-ESRGAN)](#5-image-enhancement-real-esrgan)
-   [AI Beautify](#6-ai-beautify)
-   [Background Replacement](#7-background-replacement)
-   [Style Transfer](#8-style-transfer)
-   [Comic Generation](#9-comic-generation)
-   [Story Comic (Multi-page)](#10-story-comic-multi-page)

---

## Authentication

Currently, no authentication is required. API keys may be added in future versions.

---

## Rate Limiting

**All endpoints** are rate-limited to prevent abuse:

-   **Limit**: 60 requests per minute per IP address
-   **Window**: 60 seconds
-   **Response when exceeded**: HTTP 429 (Too Many Requests)

```json
{
    "success": false,
    "error": "Too many requests",
    "code": "RATE_LIMIT_EXCEEDED",
    "retryAfter": 42
}
```

---

## Response Format

### Standard Success Response

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        // Response data specific to endpoint
    },
    "meta": {
        // Additional metadata
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### Standard Error Response

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "Error description",
        "code": "ERROR_CODE",
        "details": "Additional details"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

---

## Error Handling

### Common Error Codes

| Code                  | HTTP Status | Description                |
| --------------------- | ----------- | -------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   |
| `MISSING_FILE`        | 400         | Required file not provided |
| `PROCESSING_ERROR`    | 400         | Processing failed          |
| `NOT_FOUND`           | 404         | Resource not found         |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          |
| `INTERNAL_ERROR`      | 500         | Server error               |
| `REPLICATE_ERROR`     | 500         | AI model processing failed |

---

## API Endpoints

## 1. Manifest Management

Quản lý danh sách resource packs (styles, backgrounds, etc.)

### 1.1 Get Manifest List

**Endpoint**: `GET /manifest`

**Chức năng**: Lấy danh sách resource packs với filter và phân trang

**Query Parameters**:
| Parameter | Type | Required | Description | Default |
| ----------- | ------ | -------- | ------------------------------ | ------- |
| `category` | String | ❌ | Filter theo category | - |
| `target` | String | ❌ | Filter theo target feature | - |
| `page` | Number | ❌ | Số trang (≥ 1) | `1` |
| `page_size` | Number | ❌ | Items per page (1-500) | `50` |

**Response Success (200)**:

```json
{
    "request_id": "req_abc123",
    "status": "success",
    "data": {
        "items": [
            {
                "id": "styles/anime",
                "title": "styles — anime",
                "category": "styles",
                "target": "anime",
                "count": 15,
                "files": [
                    {
                        "key": "ON1_BW_LUTs/For_Other_Programs/BW1.cube",
                        "size": 885033,
                        "etag": "a70b6f392f7ef8850c02ed1065f8674b",
                        "content_type": "application/octet-stream"
                    }
                ]
            }
        ]
    },
    "pagination": {
        "total": 42,
        "page": 1,
        "page_size": 50,
        "total_pages": 1
    },
    "meta": {
        "version": "2025.10.0"
    }
}
```

### 1.2 Get Presigned URL

**Endpoint**: `POST /presign`

**Chức năng**: Tạo presigned URL để download file từ pack

**Content-Type**: `application/json`

**Request Body**:

```json
{
    "pack_id": "styles/anime",
    "key": "styles/anime/01.jpg"
}
```

**Response Success (200)**:

```json
{
    "request_id": "e2aYWej9VHWu60oNS9DKE",
    "status": "success",
    "data": {
        "url": "https://d658d7ec8dd0cbdd02ce985566c8a042.r2.cloudflarestorage.com/filters-prod/ON1_BW_LUTs/For_Other_Programs/BW1.cube?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Credential=...",
        "expires_in": 3600
    }
}
```

---

## 2. Image Upscaling (GFPGAN)

Face restoration và upscaling chuyên biệt cho ảnh chân dung

### 2.1 Upscale Image

**Endpoint**: `POST /upscale`

**Chức năng**: Khôi phục và tăng chất lượng khuôn mặt trong ảnh

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| --------- | ------ | -------- | ------------------------------------ | -------- |
| `image` | File | ✅ | File ảnh (JPEG, PNG, WebP) | - |
| `scale` | Number | ❌ | Hệ số scale: `1`, `2`, `4` | `2` |
| `version` | String | ❌ | Model version: `"v1.3"`, `"v1.4"` | `"v1.4"` |

**Constraints**:

-   Max file size: 10MB
-   Supported formats: JPEG, PNG, WebP

**Response Success (200)**:

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
    }
}
```

**Processing Time**: 15-90 seconds (tùy kích thước ảnh)

**Use Cases**:

-   Khôi phục ảnh cũ
-   Cải thiện ảnh profile
-   Enhance ảnh chân dung
-   Sửa ảnh bị mờ/nhiễu

---

## 3. Portrait Relighting (IC-Light)

Text-guided relighting cho ảnh chân dung

### 3.1 Relight Portrait

**Endpoint**: `POST /portraits/ic-light`

**Chức năng**: Thay đổi ánh sáng của ảnh chân dung theo mô tả văn bản

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| ------------------- | ------ | -------- | ---------------------------------------------------- | ----------------------------------------------- |
| `image` | File | ✅ | File ảnh (JPEG, PNG, WebP) | - |
| `prompt` | String | ✅ | Mô tả ánh sáng mong muốn | `"studio soft light, flattering portrait lighting"` |
| `appended_prompt` | String | ❌ | Text thêm vào cuối prompt | `"best quality"` |
| `negative_prompt` | String | ❌ | Mô tả những gì muốn tránh | `"lowres, bad anatomy, bad hands, cropped, worst quality"` |
| `light_source` | String | ✅ | `"None"`, `"Left Light"`, `"Right Light"`, `"Top Light"`, `"Bottom Light"` | `"None"` |
| `steps` | Number | ❌ | Số steps inference (1-100) | `25` |
| `cfg` | Number | ❌ | Guidance scale (1-32) | `2` |
| `width` | Number | ❌ | Chiều rộng output (256-1024, step 64) | Auto |
| `height` | Number | ❌ | Chiều cao output (256-1024, step 64) | Auto |
| `number_of_images` | Number | ❌ | Số lượng ảnh output (1-12) | `1` |
| `output_format` | String | ❌ | `"webp"`, `"jpg"`, `"png"` | `"webp"` |
| `output_quality` | Number | ❌ | Chất lượng output (1-100) | `80` |

\*Note: Phải cung cấp `image` HOẶC `image_url`

**Response Success (200)**:

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "outputs": [
            {
                "url": "https://replicate.delivery/pbxt/xyz123.webp",
                "index": 0
            },
            {
                "url": "https://replicate.delivery/pbxt/abc456.webp",
                "index": 1
            }
        ]
    },
    "meta": {
        "model": "jagilley/controlnet-hough",
        "prompt": "studio soft light, flattering portrait lighting best quality",
        "light_source": "Left Light",
        "steps": 25,
        "cfg": 2,
        "dimensions": {
            "width": 768,
            "height": 1024
        },
        "output_format": "webp",
        "number_of_images": 2
    }
}
```

**Processing Time**: 30-120 seconds

**Use Cases**:

-   Re-light portrait shoots
-   Fix poorly lit photos
-   Create lighting variations
-   Artistic lighting effects

---

## 4. Clarity Improvement (Real-ESRGAN)

Super-resolution và tăng độ sắc nét cho ảnh

### 4.1 Improve Clarity

**Endpoint**: `POST /clarity`

**Chức năng**: Tăng độ phân giải và độ sắc nét của ảnh

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| ------------- | ------- | -------- | ---------------------------------- | ------- |
| `image` | File | ✅ | File ảnh (JPEG, PNG, WebP) | - |
| `scale` | Number | ❌ | Hệ số scale: `2`, `4` | `2` |
| `faceEnhance` | Boolean | ❌ | Bật face enhancement | `false` |

**Response Success (200)**:

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
        "input_size": {
            "width": 1024,
            "height": 768
        },
        "output_size": {
            "width": 2048,
            "height": 1536
        }
    }
}
```

**Processing Time**: 20-120 seconds

**Use Cases**:

-   Upscale old photos
-   Improve scanned images
-   Enhance low-resolution photos
-   Prepare images for print

---

## 5. Image Enhancement (Real-ESRGAN)

Tăng độ phân giải và sắc nét với model `nightmareai/real-esrgan`.

### 5.1 Enhance Image

**Endpoint**: `POST /enhance`

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| --------- | ---- | -------- | ------------------------------------------ | ------- |
| `image` | File | ✅ | File ảnh (JPEG, PNG, WebP) | - |
| `scale` | Number | ❌ | Hệ số scale: `2` hoặc `4` | `2` |
| `face_enhance` | Boolean | ❌ | Bật bổ trợ khuôn mặt (alias: `faceEnhance`) | `false` |
| `model` | String | ❌ | Giữ cho tương thích cũ, chỉ nhận `real-esrgan` | `real-esrgan` |
| _Note_ | - | - | Ảnh được prescale xuống tối đa 2560px **hoặc ~2MP** để tránh lỗi GPU | - |

**Response Success (200)**:

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
        "model": "nightmareai/real-esrgan",
        "scale": 4,
        "faceEnhance": true
    }
}
```

**Processing Time**: 15-60 seconds (tùy scale và kích thước input)

**Use Cases**:

-   Upscale ảnh sản phẩm / marketing
-   Cải thiện ảnh chân dung (bật `face_enhance`)
-   Chuẩn bị ảnh in ấn (scale 4x)
-   Nâng độ rõ nét cho ảnh cũ/quét

---

## 6. AI Beautify

High-quality image super-resolution bằng model `cjwbw/real-esrgan` trên Replicate

### 6.1 Beautify Portrait

**Endpoint**: `POST /ai-beautify`

**Chức năng**: Real-ESRGAN super-resolution với tùy chọn `scale` (2-4, mặc định 2)

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| --------- | ---- | -------- | -------------------------- | ------- |
| `image` | File | ✅ | File ảnh (JPEG, PNG, WebP) | - |
| `scale` | Number | ❌ | 2-4, hệ số upscale | 2 |

**Constraints**:

-   Max file size: 10MB
-   Supported formats: JPEG, PNG, WebP
-   Images > ~2MP will be auto-resized to fit GPU memory
-   Scale 4 có thể tốn thời gian hơn scale 2

**Response Success (200)**:

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "key": "aiBeautify/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://your-public-url.com/aiBeautify/...",
        "presigned_url": "https://pub-xxxx.r2.dev/aiBeautify/...",
        "expires_in": 3600
    },
    "meta": {
        "model": "cjwbw/real-esrgan",
        "version": "42fed1c4974146d4d2414e2be2c5277c7fcf05fcc3a73abf41610695738c1d7b",
        "scale": 2,
        "bytes": 245678,
        "requestId": "req_abc123xyz",
        "pipeline": ["cjwbw/real-esrgan"]
    }
}
```

**Processing Pipeline**:

1. Validate input (mime, size, scale)
2. Run `cjwbw/real-esrgan` (Real-World Blind Super-Resolution) trên Replicate
3. Upload output lên R2 (`aiBeautify/` prefix)
4. Trả presigned + public URL

**Processing Time**: ~10-15 seconds (phụ thuộc scale/kích thước input)

**Use Cases**:

-   Image super-resolution và upscaling
-   Cải thiện ảnh chất lượng thấp
-   E-commerce product photography
-   Print preparation (scale 4)

---

## 7. Background Replacement

Xóa nền hoặc thay thế nền ảnh bằng AI

### 7.1 Replace/Remove Background

**Endpoint**: `POST /replace-bg`

**Chức năng**: Xóa nền (remove mode) hoặc thay thế nền (replace mode)

**Content-Type**: `multipart/form-data`

**Request Parameters**:

#### Remove Mode

| Parameter   | Type   | Required | Description                                  | Default     |
| ----------- | ------ | -------- | -------------------------------------------- | ----------- |
| `fg`        | File   | ✅       | Ảnh cần xóa nền                              | -           |
| `mode`      | String | ❌       | `"remove"`                                   | `"replace"` |
| `featherPx` | Number | ❌       | Độ mượt viền alpha (0-20)                    | `1`         |
| `signTtl`   | Number | ❌       | Thời gian sống presigned URL (60-86400 giây) | `3600`      |

#### Replace Mode

| Parameter   | Type   | Required | Description                                                | Default     |
| ----------- | ------ | -------- | ---------------------------------------------------------- | ----------- |
| `fg`        | File   | ✅       | Ảnh foreground (ảnh muốn giữ)                              | -           |
| `bg`        | File   | ✅       | Ảnh background (nền mới)                                   | -           |
| `mode`      | String | ❌       | `"replace"`                                                | `"replace"` |
| `fit`       | String | ❌       | `"cover"`, `"contain"`, `"fill"`, `"inside"`, `"outside"`  | `"cover"`   |
| `position`  | String | ❌       | `"centre"`, `"top"`, `"bottom"`, `"left"`, `"right"`, etc. | `"centre"`  |
| `featherPx` | Number | ❌       | Độ mượt viền alpha (0-20)                                  | `1`         |
| `shadow`    | String | ❌       | Thêm bóng đổ: `"0"` (không), `"1"` (có)                    | `"1"`       |
| `signTtl`   | Number | ❌       | Thời gian sống presigned URL (60-86400 giây)               | `3600`      |

**Response Success (200)**:

```json
{
    "success": true,
    "requestId": "req_abc123",
    "data": {
        "key": "images/2025/11/18/uuid-abc123.png",
        "url": "https://pub-xxxx.r2.dev/images/2025/11/18/uuid-abc123.png",
        "presignedUrl": "https://pub-xxxx.r2.dev/images/2025/11/18/uuid-abc123.png?X-Amz-...",
        "expiresIn": 3600,
        "meta": {
            "width": 1024,
            "height": 768,
            "mode": "replace"
        }
    }
}
```

**Processing Time**: 20-60 seconds

**Use Cases**:

-   Product photography (e-commerce)
-   Portrait background replacement
-   Remove distracting backgrounds
-   Create marketing materials

---

## 8. Style Transfer

Chuyển đổi ảnh sang các phong cách nghệ thuật khác nhau

### 8.1 Apply Style

**Endpoint**: `POST /style`

**Chức năng**: Biến đổi ảnh sang phong cách nghệ thuật (anime, watercolor, oil painting, etc.)

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| --------- | ------ | -------- | ------------------------------------------------- | ------- |
| `image` | File | ✅ | File ảnh (JPEG, PNG, WebP) | - |
| `style` | String | ✅ | Phong cách (xem bảng dưới) | - |
| `extra` | String | ❌ | Mô tả bổ sung (VD: "add sunset background") | - |

**Supported Styles**:
| Style | Description | Best For |
| -------------- | --------------------------------------------- | --------------------------- |
| `anime` | Anime cel-shaded với clean outlines | Portraits, characters |
| `ghibli` | Studio Ghibli watercolor style | Landscapes, nostalgic scenes|
| `watercolor` | Watercolor painting effect | Artistic portraits |
| `oil-painting` | Classical oil painting on canvas | Fine art, portraits |
| `sketches` | Colored pencil sketch style | Quick artistic renditions |
| `cartoon` | 1990s animated cartoon style | Fun, playful transformations|

**Response Success (200)**:

```json
{
    "success": true,
    "requestId": "req_abc123",
    "data": {
        "key": "styles/anime/550e8400-e29b-41d4-a716-446655440000.jpg",
        "url": "https://pub-xxxx.r2.dev/styles/anime/550e8400-e29b-41d4-a716-446655440000.jpg",
        "presignedUrl": "https://pub-xxxx.r2.dev/styles/anime/...?X-Amz-Algorithm=...",
        "expiresIn": 3600,
        "meta": {
            "style": "anime",
            "bytes": 245678,
            "requestId": "req_abc123"
        }
    }
}
```

**Processing Time**: 30-150 seconds

**Use Cases**:

-   Transform photos into art
-   Create unique avatars/profile pictures
-   Social media content
-   Marketing materials

---

## 9. Comic Generation

Tạo truyện tranh anime nhiều trang với thoại tiếng Việt từ prompt văn bản

### 9.1 Generate Comic

**Endpoint**: `POST /comic/generate`

**Chức năng**: Tạo comic nhiều trang (1-3 trang) với AI, mỗi trang có 3-9 panel, thoại tiếng Việt overlay trên ảnh

**Content-Type**: `multipart/form-data`

**Request Body (form-data fields)**:

```
prompt=Một cô gái phát hiện ra cổng thần bí trong khu rừng
pages=2
panelsPerPage=4
style=anime
```

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| --------- | ------ | -------- | -------------------------------- | --------------- |
| `prompt` | String | ✅ | Mô tả câu chuyện (≥ 5 ký tự) | - |
| `pages` | Number | ❌ | Số trang (1-3) | `2` |
| `panelsPerPage` | Number | ❌ | Số panel mỗi trang (3-9) | `4` |
| `style` | String | ❌ | Phong cách: `"anime"`, `"manga"`, `"webtoon"` | `"anime"` |

**Response Success (200)**:

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "data": {
        "comic_id": "550e8400-e29b-41d4-a716-446655440000",
        "image": {
            "key": "comics/550e8400-1732694400000/comic.png",
            "url": "https://pub-xxxx.r2.dev/comics/550e8400-1732694400000/comic.png",
            "presigned_url": "https://pub-xxxx.r2.dev/comics/...?X-Amz-Algorithm=...",
            "expires_in": 3600
        },
        "panels": [
            {
                "page": 1,
                "panel": 1,
                "description_vi": "Cô gái đi bộ trong khu rừng rậm rạp",
                "description_en": "A girl walking through a dense forest",
                "dialogue": "Chỗ này trông kỳ lạ quá...",
                "speaker": "Mai",
                "emotion": "curious"
            },
            {
                "page": 1,
                "panel": 2,
                "description_vi": "Cổng đá phát sáng xuất hiện",
                "description_en": "A glowing stone portal appears",
                "dialogue": "Cái gì đây?!",
                "speaker": "Mai",
                "emotion": "surprised"
            }
        ]
    },
    "meta": {
        "pages": 2,
        "panels_per_page": 4,
        "total_panels": 8,
        "style": "anime",
        "model": {
            "storyboard": "google/gemini-2.5-flash",
            "image": "google/nano-banana",
            "overlay": "sharp + SVG"
        },
        "processing_time_ms": 45230
    }
}
```

**Processing Pipeline** (Hybrid Approach):

1. **Gemini 2.5 Flash**: Tạo storyboard JSON với thoại tiếng Việt
    - Output: `{ panels: [{ description_vi, description_en, dialogue, speaker, emotion }] }`
2. **Nano Banana**: Sinh ảnh comic layout nhiều trang (chỉ dùng description_en)
    - Prompt format: "two comic book pages side by side, ### Page 1\n**Panel 1** _Description:_ ..."
3. **SVG Overlay**: Composite bong bóng thoại tiếng Việt lên ảnh
    - Font: Noto Sans, Segoe UI (hỗ trợ tiếng Việt)
    - Auto-positioning dựa trên layout grid
4. **Output**: Ảnh PNG hoàn chỉnh (nhiều trang xếp ngang, tỷ lệ 2:3)

**Processing Time**: 45-120 seconds (tùy số trang × panels)

**Layout Structure**:

-   **1 page**: Trang đơn, các panel xếp dọc
-   **2 pages**: 2 trang xếp ngang (side-by-side)
-   **3 pages**: 3 trang xếp ngang (horizontal strip)

**Use Cases**:

-   Comic tiếng Việt cho thị trường Việt Nam
-   Visual storytelling với nhiều trang
-   Educational comics
-   Social media content (Instagram carousel, Facebook posts)

---

## 10. Story Comic (Multi-page)

Tạo truyện tranh anime màu nhiều trang (2–3 trang), mỗi trang 3–4 panel với thoại tiếng Việt và bong bóng lời.

### 10.1 Generate Story Comic

**Endpoint**: `POST /story-comic/generate`

**Chức năng**: Từ một prompt duy nhất, sinh outline → storyboard từng trang → ảnh panel (Animagine) → ghép trang và trả URL từng page.

**Content-Type**: `application/json` (hỗ trợ cả `multipart/form-data` dạng text)

**Request Body**:

```
{
  "prompt": "Một nữ sinh nhút nhát gặp mèo phép thuật trong đêm mưa ở Tokyo.",
  "pages": 3,
  "panels_per_page": 4,
  "style_selector": "Anime",
  "quality_selector": "Standard v3.1"
}
```

**Request Parameters**:
| Parameter | Type | Required | Description | Default |
| --------- | ---- | -------- | ----------- | ------- |
| `prompt` | String | ✅ | Mô tả câu chuyện (≥ 8 ký tự) | - |
| `pages` | Number | ❌ | 2 hoặc 3 trang | `3` |
| `panels_per_page` | Number | ❌ | 3 hoặc 4 panel mỗi trang | `4` |
| `style_selector` | Enum | ❌ | Preset style Animagine: `(None)`, `Cinematic`, `Photographic`, `Anime`, `Manga`, `Digital Art`, `Pixel art`, `Fantasy art`, `Neonpunk`, `3D Model` | `(None)` |
| `quality_selector` | Enum | ❌ | Preset quality: `(None)`, `Standard v3.0`, `Standard v3.1`, `Light v3.1`, `Heavy v3.1` | `Standard v3.1` |

**Response Success (200)**:

```json
{
  "request_id": "req_abc123xyz",
  "status": "success",
  "story_id": "story-nu-sinh-meo-phep",
  "pages": [
    {
      "page_index": 0,
      "page_url": "https://pub-xxxx.r2.dev/comics/story-nu-sinh-meo-phep/page-0.png",
      "key": "comics/story-nu-sinh-meo-phep/page-0.png",
      "presigned_url": "https://pub-xxxx.r2.dev/comics/.../page-0.png?...",
      "panels": [
        { "id": 1, "dialogue": "Trời mưa mãi...", "speaker": "Yuki", "emotion": "sad" }
      ]
    },
    {...}
  ],
  "meta": {
    "outline": [{ "id": 1, "summary_vi": "Yuki đi bộ dưới mưa", "main_emotion": "sad" }],
    "pages": [{ "page_index": 0, "beats": [1, 2, 3, 4], "panel_count": 4 }],
    "model": {
      "llm": "google/gemini-2.5-flash",
      "image": "cjwbw/animagine-xl-3.1",
      "style_selector": "Anime",
      "quality_selector": "Standard v3.1"
    }
  }
}
```

**Processing Pipeline**:

1. Gemini 2.5 Flash tạo outline 9–12 beat, ép JSON sạch.
2. Chia beat theo số trang (3–4 beat/trang).
3. Gemini storyboard từng trang (panel description + prompt_tags Danbooru + dialogue tiếng Việt).
4. Animagine XL 3.1 sinh ảnh panel (anime màu, không manga).
5. Render trang (layout 3–4 panel, speech bubbles), upload R2.

**Processing Time**: ~90–240s cho 3 trang x 4 panel (phụ thuộc số trang/panel).

**Use Cases**:

-   Mini story nhiều trang
-   Social content dài hơi
-   Pitch/outline truyện kèm hình ảnh
-   Giáo dục minh họa nhiều bước

---

## 📊 Quick Comparison Table

| Feature        | Endpoint                   | Input            | Main Function                     | Processing Time |
| -------------- | -------------------------- | ---------------- | --------------------------------- | --------------- |
| Manifest       | GET /manifest              | Query params     | List resource packs               | < 1s            |
| GFPGAN         | POST /upscale              | Image file       | Face restoration & upscaling      | 15-90s          |
| IC-Light       | POST /portraits/ic-light   | Image file       | Portrait relighting               | 30-120s         |
| Clarity        | POST /clarity              | Image file       | Super-resolution                  | 20-120s         |
| Enhance        | POST /enhance              | Image file       | Professional enhancement          | 30-180s         |
| AI Beautify    | POST /ai-beautify          | Image file       | Real-ESRGAN super-resolution      | 10-15s          |
| Replace BG     | POST /replace-bg           | Image file(s)    | Remove/replace background         | 20-60s          |
| Style Transfer | POST /style/replace-style  | Image file       | Artistic style transformation     | 30-150s         |
| Comic Generate | POST /comic/generate       | Form-data (text) | Auto comic generation             | 60-240s         |
| Story Comic    | POST /story-comic/generate | JSON/form (text) | Multi-page anime comic w/ bubbles | 90-240s         |

---

## 🔒 Storage & URLs

### Cloudflare R2 Storage

Tất cả output images được lưu trên Cloudflare R2:

-   **Public URLs**: Có thể dùng lâu dài
-   **Presigned URLs**: Expires sau 3600 giây (1 giờ) theo mặc định
-   **Key format**: `{feature}/{date}/{uuid}.{ext}`

### URL Expiration

⚠️ **Quan trọng cho FE**:

-   Presigned URLs chỉ tồn tại trong thời gian giới hạn
-   Nên download và lưu ảnh ngay nếu cần dùng lâu dài
-   Không cache presigned URLs > 30 phút
-   Sử dụng public URLs nếu có (không expire)

---

## 📝 Best Practices

### For Frontend Development

1. **Error Handling**

    - Luôn kiểm tra `status` field trong response
    - Hiển thị `error.message` cho user
    - Log `request_id` để debug

2. **File Upload**

    - Validate file size < 10MB trước khi upload
    - Validate file type (JPEG, PNG, WebP)
    - Hiển thị progress bar cho long-running requests

3. **Rate Limiting**

    - Implement client-side rate limiting
    - Hiển thị `retryAfter` thời gian cho user
    - Queue requests nếu cần batch processing

4. **Loading States**

    - Hiển thị loading indicator (processing time 15s-240s)
    - Cho phép user cancel request nếu quá lâu
    - Hiển thị estimated time nếu có thể

5. **Presigned URLs**

    - Download ảnh ngay sau khi nhận response
    - Không lưu presigned URLs vào database
    - Sử dụng public URLs khi có thể

6. **Request IDs**
    - Lưu `request_id` cho mỗi request
    - Gửi kèm khi report bugs
    - Sử dụng để track processing status

---

## 🛠️ Development Tips

### Testing Endpoints

```bash
# Test with curl
curl -X POST http://localhost:3000/api/upscale \
  -F "image=@test.jpg" \
  -F "scale=2"

# Test rate limiting
for i in {1..65}; do curl http://localhost:3000/api/manifest; done
```

### Common Pitfalls

❌ **Don't**:

-   Upload files > 10MB
-   Request same image nhiều lần liên tục
-   Cache presigned URLs > 30 phút
-   Ignore rate limit errors
-   Skip validation trước khi call API

✅ **Do**:

-   Validate inputs client-side trước
-   Implement retry với exponential backoff
-   Handle all error codes properly
-   Show meaningful error messages
-   Use request_id for debugging

---

## 📞 Support & Documentation

### Detailed Documentation

Mỗi feature có tài liệu chi tiết tại:

-   `/src/features/{feature}/API.md`
-   `/src/features/{feature}/README.md`

### Need Help?

1. Check feature-specific documentation
2. Review error messages và request_id
3. Verify input parameters
4. Check rate limiting status
5. Contact backend team với request_id

---

## 📅 Changelog

### v1.0.0 (November 18, 2025)

-   Initial API release
-   9 main features
-   Rate limiting: 60 req/min
-   R2 storage integration
-   Presigned URL support

---

**Documentation Version**: 1.0.0  
**API Version**: v1  
**Last Updated**: November 18, 2025
