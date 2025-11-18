# AI Comic Generator – README cho Agent (1 API duy nhất)

## 1. Mục tiêu & Flow tổng

Mình muốn một **API đơn giản**:

> **Client gửi prompt → Server trả về ngay 1 ảnh comic (có nhiều panel + bong bóng lời thoại).**

Không cần bước “xem/stored storyboard” riêng. Storyboard vẫn có, nhưng chỉ dùng **nội bộ** trong API.

### Flow bên trong (1 API):

1. Nhận `prompt` từ client (tiếng Việt).
2. Gọi **LLM (Gemini 2.5 Flash)** → sinh **storyboard JSON**:
    - Danh sách panel
    - Lời thoại (tiếng Việt) cho từng panel
    - Prompt tags (tiếng Anh, Danbooru-style) cho model ảnh
3. Gọi **image model Animagine XL 3.1** trên Replicate:
    - Sinh ảnh anime màu cho từng panel (không có chữ, không bubble).
4. Dùng **node-canvas**:
    - Ghép tất cả panel thành **một page comic** (1 ảnh)
    - Vẽ **bong bóng lời thoại** + text tiếng Việt
5. Upload ảnh final lên **Cloudflare R2**, trả về **URL** cho client.

---

## 2. Kiến trúc backend hiện có (để agent bám theo)

-   **Node.js + Express (ESM)** – dùng `import`/`export`
-   Kiểu dự án: **feature-first**

Thư mục quan trọng:

-   `config/` – config, env, PERF…
-   `middlewares/` – error handler, upload, security, request-id, timeout…
-   `integrations/`
    -   `replicate/` – client gọi Replicate
    -   `r2/` – upload & presign Cloudflare R2
-   `features/` – mỗi feature 1 folder:
    -   `replace-bg/`, `manifest/`, `presign/`, ...
    -   **Thêm mới**: `comic/`
-   `utils/`
    -   `retry.js` – `withRetry`
    -   `limiters.js` – `withReplicateLimiter` (p-limit)
    -   `asyncHandler`, `image`, ...

**Nguyên tắc:**

-   Mọi I/O async.
-   Gọi model qua `withReplicateLimiter`.
-   Có retry/timeout cho call mạng.
-   API trả JSON rõ ràng, có `request_id` và log bằng pino.

Agent phải **giữ đúng style này**.

---

## 3. Model sử dụng

### 3.1. LLM – Storyboard + Prompt

-   **Model**: `google/gemini-2.5-flash` trên Replicate
-   Vai trò:
    -   Nhận `prompt` tiếng Việt từ user
    -   Sinh **1 object JSON** (storyboard) gồm:
        -   `characters`: danh sách nhân vật
        -   `panels`: mỗi panel có:
            -   `id`
            -   `description_vi` – mô tả cảnh tiếng Việt (debug)
            -   `prompt_tags` – chuỗi Danbooru tags tiếng Anh cho Animagine
            -   `dialogue` – lời thoại tiếng Việt
            -   `speaker` – tên nhân vật nói
            -   `emotion` – `happy|sad|angry|surprised|neutral`

**Yêu cầu với Gemini:**

-   Output là **JSON thuần**, không kèm text giải thích.
-   Có thể parse bằng `JSON.parse`.
-   `prompt_tags` phải hợp với style **Animagine XL 3.1** (anime, màu).

### 3.2. Image – Anime Comic (KHÔNG manga)

-   **Model**: `cjwbw/animagine-xl-3.1` trên Replicate
-   Mục tiêu: sinh ảnh **anime màu**, kiểu illustration/comic, **không** manga đen trắng.

**Input mặc định cho mỗi panel (ý tưởng):**

-   `prompt`: lấy từ `panel.prompt_tags` + thêm style chung, ví dụ:
    -   `"masterpiece, best quality, anime style, vibrant colors, detailed background, <prompt_tags>, no text, no speech bubble"`
-   `negative_prompt`:
    -   `"nsfw, lowres, text, logo, watermark, signature, speech bubble, caption, bad hands, extra fingers, deformed, extra limbs"`
-   `width`: khoảng `832` (hoặc tương đương)
-   `height`: khoảng `1216` (tỉ lệ đứng, panel dọc)
-   `num_inference_steps`: 24–30
-   `guidance_scale`: 6–8
-   (Optional) `seed`: có thể cố định trong 1 page để cùng vibe.

**Quan trọng:**

-   Không dùng tag `manga`, `screentone`, `lineart only`.
-   Luôn loại chữ khỏi ảnh (để backend tự vẽ bubble).

---

## 4. API duy nhất: `/api/comic/generate`

### 4.1. Endpoint

`POST /api/comic/generate`

#### Request body (từ client)

```json
{
    "prompt": "Một cô nữ sinh nhút nhát gặp một con mèo phép thuật trong đêm mưa ở Tokyo.",
    "panels": 4,
    "style": "anime_color"
}
```

prompt: mô tả ngắn truyện (VN).

panels: số panel muốn trên 1 page (1–6). V1 cho phép 3–4 panel cũng được.

style: hiện tại chỉ dùng "anime_color" (không manga).

4.2. Response body (trả cho client)

```json
{
    "request_id": "xxx",
    "page_url": "https://r2.example.com/comics/abc123/page-0.png",
    "meta": {
        "panels": [
            {
                "id": 1,
                "dialogue": "Trời mưa hoài... Mình ghét cảm giác cô đơn này.",
                "speaker": "Yuki",
                "emotion": "sad"
            },
            {
                "id": 2,
                "dialogue": "Hửm? Một con mèo... Ở đây sao?",
                "speaker": "Yuki",
                "emotion": "surprised"
            }
        ]
    }
}
```

Client chỉ cần lấy page_url và show ra như 1 ảnh comic duy nhất.

5. Logic chi tiết bên trong /api/comic/generate

Tất cả nằm trong 1 request:

Bước 1 – Gọi Gemini → storyboard JSON

Build system prompt (tiếng Việt/Anh đều được) cho Gemini:

Giải thích nhiệm vụ:

Nhận prompt (VN), panels (số panel).

Sinh 1 JSON với các field:

characters[]

panels[] (như mô tả trong phần LLM).

prompt_tags phải là Danbooru tags tiếng Anh cho anime (hợp với Animagine 3.1).

Lời thoại (dialogue) tiếng Việt, tự nhiên, ngắn gọn.

Yêu cầu: chỉ return JSON, không text khác.

Gọi google/gemini-2.5-flash trên Replicate.

Parse JSON.parse(result) → lấy storyboard.

Bước 2 – Gọi Animagine → ảnh từng panel

Với mỗi panel trong storyboard.panels:

Build prompt:

prompt = "<panel.prompt_tags>, anime style, vibrant colors, high quality, detailed background, no text, no speech bubble"

Gọi cjwbw/animagine-xl-3.1 qua Replicate:

Input như phần 3.2

Nhận URL ảnh panel, lưu vào array, ví dụ:

const renderedPanels = [
{
...panel,
image_url: "https://replicate.delivery/.../panel-1.png"
},
...
];

Dùng withReplicateLimiter + Promise.allSettled hoặc tương tự để generate song song có kiểm soát.

Bước 3 – Ghép page + vẽ bong bóng thoại (node-canvas)

Chọn kích thước page, ví dụ:

width = 1080

height = 1620 (tỉ lệ 2:3)

Chọn layout theo số panel, ví dụ 4 panel:

const LAYOUTS = {
4: [
{ x: 0, y: 0, w: 540, h: 810 },
{ x: 540, y: 0, w: 540, h: 810 },
{ x: 0, y: 810, w: 540, h: 810 },
{ x: 540, y: 810, w: 540, h: 810 }
]
};

Dùng @napi-rs/canvas (hoặc canvas):

Tạo canvas, fill background (ví dụ xám đậm).

Loop qua renderedPanels:

loadImage(panel.image_url) → drawImage vào vị trí layout tương ứng.

Gọi drawSpeechBubble(ctx, { text: panel.dialogue, box }) để vẽ bong bóng trong panel đó.

Hàm drawSpeechBubble:

Vẽ rounded rectangle trắng (fill trắng, stroke đen).

Vẽ “đuôi” tam giác hướng xuống panel.

Vẽ text với wrapText, font dễ đọc (VD: 20–24px).

Text là panel.dialogue (tiếng Việt).

canvas.toBuffer("image/png") → buffer PNG.

Bước 4 – Lưu R2 + trả URL

Đặt key, ví dụ:

key = comics/<random_story_id>/page-0.png

Gọi helper sẵn có:

await uploadBufferToR2({ key, buffer, contentType: "image/png" });
const url = await presignGetUrl({ key });

Trả JSON response:

```json
{
"request_id": "<x-request-id>",
"page_url": "<url>",
"meta": {
"panels": [ ...id/dialogue/speaker... ]
}
}
```

6. Yêu cầu dành cho AI Agent

Chỉ 1 API duy nhất:

Tạo feature comic với endpoint POST /api/comic/generate.

Bên trong tự gọi Gemini + Animagine + render + R2.

Không sinh phong cách manga:

Không dùng manga, screentone, black and white trong prompt_tags.

Style chính là anime màu.

Gemini output = JSON sạch:

Không text dư thừa.

Đảm bảo parse được qua JSON.parse.

Dùng đúng kiến trúc backend:

Tạo folder: src/features/comic/

comic.routes.js

comic.controller.js

comic.service.js (hoặc tách story & render bên trong)

Gọi Replicate qua integrations/replicate/client.js

Gọi R2 qua integrations/r2/storage.service.js

Dùng withReplicateLimiter, withRetry, logging pino.

Hiệu năng & lỗi:

Generate panel song song nhưng có giới hạn (p-limit).

Xử lý timeout/lỗi từ Replicate (trả JSON error chuẩn).

Giữ thời gian response hợp lý (tuỳ thuộc số panel và steps).

Code rõ ràng, dễ plug-in:

Tách logic “gọi Gemini”, “gọi Animagine”, “render page”, “lưu R2” thành hàm riêng.

Comment ngắn gọn ở chỗ quan trọng.
