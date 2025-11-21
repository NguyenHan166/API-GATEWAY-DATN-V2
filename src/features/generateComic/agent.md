Bạn đang làm việc trong một project backend Node.js/Express (ESM) cho app chỉnh sửa ảnh AI.

## 1. Bối cảnh & kiến trúc hiện có

**Tech & kiến trúc:**

-   Node.js + Express, **ESM** (`import`/`export`).
-   Kiểu tổ chức: **feature-first**:
    -   `config/` – cấu hình, PERF, env
    -   `middlewares/` – error handler, upload, security, request-id, timeout…
    -   `integrations/`:
        -   `replicate/` – client gọi Replicate
        -   `r2/` – Cloudflare R2 (upload, presign)
    -   `features/` – mỗi API là một feature riêng:
        -   `replace-bg/`
        -   `manifest/`
        -   `presign/`
        -   (đang chuẩn bị có) `comic/` dạng 1 trang
    -   `utils/`:
        -   `retry.js` – `withRetry`
        -   `limiters.js` – `withReplicateLimiter` (p-limit)
        -   `asyncHandler`, `image`, …

**Nguyên tắc:**

-   Mọi I/O async, không block event loop.
-   Gọi API nặng (Replicate) qua `withReplicateLimiter`.
-   Có retry + timeout cho call mạng.
-   Tất cả controller trả JSON rõ ràng, có `request_id`, log bằng pino.
-   Config đọc từ `.env` + `config/perf.js` (không hard-code).

## 2. Mục tiêu chức năng mới

TẠO MỚI **một tính năng độc lập** để sinh **truyện tranh anime màu nhiều trang (2–3 trang)**, dựa trên prompt của user.

Đây là **tính năng mới tách hẳn** với các API comic 1 trang đơn giản. Đề nghị:

-   Tạo feature mới: `src/features/story-comic/`
-   Không phá hay sửa các feature khác.

### Luồng logic yêu cầu (Hướng số 3 – Outline → Multi-page):

Từ một prompt duy nhất, backend sẽ:

1. Gọi LLM (Gemini) để sinh **OUTLINE dài** toàn bộ truyện:
    - Dạng 9–12 “beat” (đoạn) từ mở đầu → cao trào → kết.
2. Chia OUTLINE này thành **nhiều phần ứng với từng trang** (2–3 trang).
3. Với **mỗi trang**, gọi LLM lần 2:
    - Input: 3–4 beat (từ outline).
    - Output: **storyboard cho 1 trang comic** (3–4 panel) ở dạng JSON:
        - Mô tả cảnh tiếng Việt
        - Lời thoại tiếng Việt
        - `prompt_tags` (Danbooru-style) tiếng Anh để feed thẳng cho model ảnh.
4. Với mỗi page JSON:
    - Gọi model ảnh **Animagine XL 3.1** trên Replicate (text-to-image) để vẽ ảnh cho từng panel (anime màu).
    - Dùng **node-canvas** (hoặc `@napi-rs/canvas`) để:
        - Ghép các panel thành **một page**.
        - Vẽ **bong bóng lời thoại** (speech bubble) + chữ tiếng Việt.
5. Upload từng page lên **Cloudflare R2**, trả về **danh sách URL page** cho client.

## 3. Model & integration

### 3.1. LLM – Gemini

-   Mặc định hiện tại: đang dùng **`google/gemini-2.5-flash` trên Replicate**.
-   Yêu cầu thiết kế code sao cho:
    -   Lớp gọi LLM được **đóng gói riêng** (VD: `storyComic.llm.js`), để sau này **dễ thay** sang gọi Gemini bằng API key trực tiếp (AI Studio) mà không phải sửa logic feature.
-   Output bắt buộc là **JSON hợp lệ**, parse được bằng `JSON.parse`.

**Bước 1 – Gọi Gemini tạo OUTLINE**

-   Input: prompt user + số trang mong muốn (2–3).
-   Output JSON (schema):

```json
{
    "story_id": "string_ngan_khong_dau_cach",
    "outline": [
        {
            "id": 1,
            "summary_vi": "mô tả ngắn gọn tiếng Việt (~1–2 câu) cho beat 1",
            "summary_en": "short English description (~1–2 sentences) for this beat",
            "main_emotion": "happy|sad|angry|surprised|neutral"
        }
    ]
}
```

Số lượng beat: khoảng 9–12 beat cho 3 trang, tuỳ prompt.

Bước 2 – Gọi Gemini tạo STORYBOARD từng trang

Cho mỗi trang, dùng 1 lần gọi LLM với:

Input:

Một số beat từ outline (3–4 beat cho 1 page).

page_index (0, 1, 2…)

Số panel mong muốn (3 hoặc 4).

Output JSON (schema):

```json
{
    "page_index": 0,
    "size": "2:3",
    "style": "anime_color",
    "panels": [
        {
            "id": 1,
            "description_vi": "mô tả tiếng Việt dễ hiểu, 1–2 câu cho cảnh panel này",
            "prompt_tags": "masterpiece, best quality, anime style, vibrant colors, detailed background, no text, no speech bubble, ...",
            "dialogue": "lời thoại tiếng Việt ngắn, tự nhiên (1–2 câu)",
            "speaker": "tên nhân vật nói",
            "emotion": "happy|sad|angry|surprised|neutral"
        }
    ]
}
```

Ràng buộc cho prompt_tags:

BẮT BUỘC phải chứa:

"masterpiece, best quality, anime style, vibrant colors, detailed background, no text, no speech bubble".

KHÔNG được chứa:

"manga", "screentone", "black and white".

Thêm tag ngắn gọn, dạng Danbooru, ví dụ:

1girl, short black hair, school uniform, rainy night, tokyo street, city lights, dynamic angle.

Ràng buộc JSON LLM:

Trả về duy nhất một object JSON đúng schema.

Không markdown, không ``` code block, không text giải thích.

Nếu cần, tạo thêm một bước “self-heal”:

Nếu output không phải JSON hợp lệ, gọi lại Gemini với prompt “hãy sửa đoạn sau thành JSON hợp lệ”.

3.2. Image model – Animagine XL 3.1

Model: cjwbw/animagine-xl-3.1 trên Replicate.

Chỉ dùng anime màu, KHÔNG manga đen trắng.

Gọi model:

prompt: từ panel.prompt_tags + thêm style chung:
"anime style, vibrant colors, high quality, detailed background, no text, no speech bubble".

negative_prompt:
"nsfw, lowres, text, logo, watermark, signature, speech bubble, caption, bad hands, extra fingers, deformed, extra limbs".

width: ~832

height: ~1216

num_inference_steps: 24–30

guidance_scale: 6–8

(optional) seed: có thể cố định theo page_index để các panel cùng vibe.

Dùng sẵn integrations/replicate/client.js + withReplicateLimiter trong utils/limiters.js.

3.3. Render page – node-canvas

Dùng @napi-rs/canvas hoặc canvas để:

Tạo canvas page, VD: width = 1080, height = 1620 (tỉ lệ 2:3).

Bố trí layout panel:

Tạo sẵn một số layout cơ bản trong file, ví dụ:

3 panel: 1 lớn trên, 2 nhỏ dưới

4 panel: grid 2x2

drawImage ảnh từng panel theo x, y, w, h.

Vẽ speech bubble:

Bong bóng: rounded rectangle trắng, viền đen.

Đuôi bong bóng: tam giác chỉ xuống panel.

Text: panel.dialogue (tiếng Việt), wrap text.

Font: 18–22px, màu đen, dễ đọc.

3.4. Cloudflare R2

Dùng helper đã tồn tại:

uploadBufferToR2({ key, buffer, contentType })

presignGetUrl({ key })

Lưu mỗi page thành 1 file:

comics/<story_id>/page-<page_index>.png

Trả về URL đã presign cho FE.

4. API design cho tính năng mới

TẠO MỚI feature: src/features/story-comic/ với cấu trúc:

storyComic.routes.js

storyComic.controller.js

storyComic.service.js (có thể tách nhỏ: outline.service, pageStoryboard.service, render.service)

storyComic.schema.js (nếu cần validate input)

4.1. Endpoint chính

POST /api/story-comic/generate

Request body:

````json
{
"prompt": "Một cô nữ sinh nhút nhát gặp một con mèo phép thuật trong đêm mưa ở Tokyo.",
"pages": 3,
"panels_per_page": 4
}

pages: 2 hoặc 3 (giới hạn, có thể default = 3).

panels_per_page: 3 hoặc 4 (default = 4).

Response JSON:
```json
{
"request_id": "x-request-id-từ middleware",
"story_id": "abc123",
"pages": [
{
"page_index": 0,
"page_url": "https://r2.../comics/abc123/page-0.png",
"panels": [
{
"id": 1,
"dialogue": "Trời mưa hoài... Mình ghét cảm giác cô đơn này.",
"speaker": "Yuki",
"emotion": "sad"
}
]
},
{
"page_index": 1,
"page_url": "https://r2.../comics/abc123/page-1.png",
"panels": [ ... ]
}
]
}
````

FE chỉ cần đọc pages[].page_url và hiển thị lần lượt như 2–3 trang truyện tranh.

4.2. Flow trong service

Trong storyComic.service.js, thiết kế một hàm tổng:

async function generateStoryComic({ prompt, pages, panelsPerPage }) {
// 1) Gọi Gemini để tạo outline
// 2) Chia outline thành outlineChunks cho từng page
// 3) For pageIndex 0..pages-1:
// - call Gemini để tạo storyboard 1 page
// - call Animagine để generate panel images
// - render page bằng node-canvas + bubble
// - upload R2, lấy page_url
// 4) Trả về JSON đúng format
}

Yêu cầu:

Dùng withReplicateLimiter khi gọi Replicate (Gemini via Replicate + Animagine).

Có retry hợp lý cho lỗi mạng.

Log error bằng logger hiện có (pino).

Validate input (pages, panels_per_page) để tránh user gửi quá lớn.

5. Yêu cầu không được quên

Tính năng này là MỚI, tách biệt:

Không được xoá/sửa các feature cũ.

Endpoint mới: POST /api/story-comic/generate.

Tự đăng ký route trong routes/index.js hoặc nơi đang gắn feature routes.

Không dùng style manga:

Không dùng từ khóa manga, screentone, black and white trong prompt_tags.

Toàn bộ truyện là anime màu.

Bắt buộc có speech bubble + lời thoại tiếng Việt:

Mọi panel có dialogue phải vẽ bong bóng tương ứng.

Font dễ đọc, bố trí vị trí hợp lý (ví dụ góc trên của panel).

LLM output JSON sạch:

Nếu Gemini hay trả thêm text → bổ sung prompt để ép “JSON only”.

Có thể thêm fallback tự sửa JSON nếu parsing lỗi.

Code theo phong cách hiện tại của project:

ESM imports.

Chia nhỏ service hợp lý, có thể test từng bước (outline, page storyboard, render, upload).

Controller dùng asyncHandler nếu project sẵn có.

Mục tiêu cuối cùng:
Khi client gọi POST /api/story-comic/generate với một prompt + pages = 2 hoặc 3, server sẽ trả về danh sách page_url của 2–3 trang comic anime màu, mỗi trang có nhiều panel, có bong bóng thoại tiếng Việt, truyện có mở–thân–kết mạch lạc, dựa trên pipeline Outline → Page Storyboard → Render như mô tả ở trên.

```

```
