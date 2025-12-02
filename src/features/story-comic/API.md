# Story Comic (Multi-page) API Documentation

## Endpoint

```
POST /api/story-comic/generate
```

## Description

Sinh truyện tranh anime màu nhiều trang (2–3 trang), mỗi trang 3–4 panel. Pipeline: Gemini tạo outline → chia beat theo trang → Gemini storyboard cho từng trang → Animagine XL 3.1 sinh ảnh panel → render trang với bong bóng thoại → upload Cloudflare R2, trả URL.

## Request

### Headers

```
Content-Type: application/json
```

> Cũng hỗ trợ `multipart/form-data` (text fields), không cần upload file.

### Body Parameters

| Parameter          | Type   | Required | Description                                                                 | Default          |
| ------------------ | ------ | -------- | --------------------------------------------------------------------------- | ---------------- |
| `prompt`           | String | ✅       | Mô tả câu chuyện (≥ 8 ký tự)                                                | -                |
| `pages`            | Number | ❌       | 2 hoặc 3 trang                                                              | `3`              |
| `panels_per_page`  | Number | ❌       | 3 hoặc 4 panel mỗi trang                                                    | `4`              |
| `style_selector`   | Enum   | ❌       | Preset style của Animagine: `(None)`, `Cinematic`, `Photographic`, `Anime`, `Manga`, `Digital Art`, `Pixel art`, `Fantasy art`, `Neonpunk`, `3D Model` | `(None)`         |
| `quality_selector` | Enum   | ❌       | Preset quality tag: `(None)`, `Standard v3.0`, `Standard v3.1`, `Light v3.1`, `Heavy v3.1`                            | `Standard v3.1`  |

### Constraints

- `pages` chỉ cho phép 2–3 để giữ thời gian xử lý hợp lý.
- `panels_per_page` chỉ cho phép 3–4.
- Không cần ảnh input; toàn bộ truyện tạo từ prompt văn bản.

### Example Request (JSON)

```bash
curl -X POST http://localhost:3000/api/story-comic/generate \
  -H "Content-Type: application/json" \
  -d '{
        "prompt": "Một nữ sinh nhút nhát gặp mèo phép thuật trong đêm mưa ở Tokyo",
        "pages": 3,
        "panels_per_page": 4,
        "style_selector": "Anime",
        "quality_selector": "Standard v3.1"
      }'
```

## Response

### Success Response (200 OK)

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
        { "id": 1, "dialogue": "Trời mưa mãi...", "speaker": "Yuki", "emotion": "sad" },
        { "id": 2, "dialogue": "Mèo ơi, em ở đâu?", "speaker": "Yuki", "emotion": "surprised" }
      ]
    },
    {
      "page_index": 1,
      "page_url": "https://pub-xxxx.r2.dev/comics/story-nu-sinh-meo-phep/page-1.png",
      "panels": [ { "id": 1, "dialogue": "...", "speaker": "Mèo", "emotion": "happy" } ]
    }
  ],
  "meta": {
    "outline": [
      { "id": 1, "summary_vi": "Yuki đi bộ dưới mưa...", "summary_en": "Yuki walks in the rain", "main_emotion": "sad" }
    ],
    "pages": [
      { "page_index": 0, "beats": [1, 2, 3, 4], "panel_count": 4 },
      { "page_index": 1, "beats": [5, 6, 7, 8], "panel_count": 4 }
    ],
    "model": {
      "llm": "google/gemini-2.5-flash",
      "image": "cjwbw/animagine-xl-3.1",
      "style_selector": "Anime",
      "quality_selector": "Standard v3.1"
    }
  }
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
    "details": {
      "prompt": { "_errors": ["prompt too short"] }
    }
  }
}
```

## Processing Pipeline (Server-side)

1. Validate input (`prompt`, `pages`, `panels_per_page`).
2. Gemini generate outline (9–12 beat), sanitize JSON.
3. Split outline theo số trang (3–4 beat/trang).
4. Gemini storyboard từng trang (panel description + dialogue + Danbooru prompt_tags).
5. Animagine XL 3.1 sinh ảnh từng panel (anime màu).
6. Render trang (layout 3–4 panel, speech bubble tiếng Việt).
7. Upload R2: `comics/{story_id}/page-{page_index}.png`, presign URL.

## Rate Limiting

- Limit: 6 requests / 60s / IP
- Response khi vượt: HTTP 429 Too Many Requests

```json
{
  "status": "error",
  "error": {
    "message": "Too many requests",
    "code": "RATE_LIMIT_EXCEEDED"
  }
}
```

## Notes & Tips

- Chỉ dùng tags anime màu; hệ thống tự loại bỏ các tag manga/screentone/black and white.
- Nếu gemini trả JSON lỗi, service tự sửa hoặc fallback để giữ pipeline chạy.
- Tổng thời gian xử lý phụ thuộc số trang/panel (ước 90–240s cho 3 trang x 4 panel).
