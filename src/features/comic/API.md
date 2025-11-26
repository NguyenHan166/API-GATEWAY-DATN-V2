# Comic Generation API Documentation

## Overview

Service tạo truyện tranh tự động từ prompt văn bản với **lời thoại tiếng Việt** sử dụng công nghệ Hybrid AI. Hệ thống kết hợp 2 AI models:

1. **Gemini 2.5 Flash** - Tạo storyboard chi tiết với lời thoại tiếng Việt tự nhiên
2. **Google Nano Banana** - Sinh ảnh comic với layout chuyên nghiệp
3. **SVG Renderer** - Overlay speech bubbles tiếng Việt lên ảnh comic

Kết quả là một ảnh comic book hoàn chỉnh với layout đẹp mắt và **lời thoại tiếng Việt** rõ ràng, phù hợp thị trường Việt Nam.

## Endpoint

```
POST /api/comic/generate
```

## Description

Tạo comic book từ prompt văn bản tiếng Việt. AI sẽ:

-   Tạo storyboard chi tiết với lời thoại tiếng Việt
-   Sinh ảnh comic layout chuyên nghiệp
-   Overlay speech bubbles tiếng Việt lên ảnh

Đây là giải pháp **Hybrid** kết hợp sức mạnh của Nano Banana (layout đẹp) với khả năng sinh lời thoại tiếng Việt tự nhiên.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters (form-data fields)

| Parameter       | Type   | Required | Description                   | Default                  |
| --------------- | ------ | -------- | ----------------------------- | ------------------------ |
| `prompt`        | String | ✅       | Mô tả câu chuyện (≥ 10 ký tự) | -                        |
| `pages`         | Number | ❌       | Số trang (1-3)                | `1`                      |
| `panelsPerPage` | Number | ❌       | Số panel mỗi trang (3-9)      | `6`                      |
| `style`         | String | ❌       | Style prefix cho comic        | `"comic book style art"` |

### Constraints

-   **Prompt length**: Tối thiểu 10 ký tự
-   **Pages**: 1-3 trang
-   **Panels per page**: 3-9 panels mỗi trang
-   **Style**: Prefix mô tả phong cách comic (ví dụ: "comic book style art", "manga style", "graphic novel art")

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "comic_url": "https://pub-xxxx.r2.dev/comics/550e8400-e29b-41d4-a716-446655440000/comic.png",
    "data": {
        "comic_id": "550e8400-e29b-41d4-a716-446655440000",
        "image": {
            "key": "comics/550e8400-e29b-41d4-a716-446655440000/comic.png",
            "url": "https://pub-xxxx.r2.dev/comics/.../comic.png",
            "presigned_url": "https://pub-xxxx.r2.dev/comics/...?X-Amz-Algorithm=..."
        },
        "panels": [
            {
                "id": 1,
                "description_vi": "Cô gái trẻ đứng trước cổng kỳ lạ phát sáng trong rừng",
                "dialogue": "Đây là gì nhỉ? Trông kỳ lạ quá!",
                "speaker": "Mai",
                "emotion": "surprised"
            },
            {
                "id": 2,
                "description_vi": "Cô bước qua cổng, ánh sáng chói lọi bao quanh",
                "dialogue": "Mình phải khám phá xem!",
                "speaker": "Mai",
                "emotion": "excited"
            }
        ]
    },
    "meta": {
        "pages": 1,
        "panelsPerPage": 6,
        "totalPanels": 6,
        "model": {
            "llm": "google/gemini-2.5-flash",
            "image": "google/nano-banana"
        }
    },
    "timestamp": "2025-11-27T10:30:00.000Z"
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
            "prompt": "prompt too short (minimum 10 characters)"
        }
    },
    "timestamp": "2025-11-27T10:30:00.000Z"
}
```

## Response Fields

| Field                      | Type   | Description                              |
| -------------------------- | ------ | ---------------------------------------- |
| `request_id`               | String | Unique request identifier                |
| `status`                   | String | "success" or "error"                     |
| `comic_url`                | String | Direct URL to comic image                |
| `data.comic_id`            | String | Unique comic identifier                  |
| `data.image.key`           | String | R2 storage key                           |
| `data.image.url`           | String | Public URL                               |
| `data.image.presigned_url` | String | Presigned URL (expires in 1 hour)        |
| `data.script`              | String | Generated comic script (markdown format) |
| `meta.pages`               | Number | Number of pages requested                |
| `meta.panelsPerPage`       | Number | Number of panels per page                |
| `meta.totalPanels`         | Number | Total panels (pages × panelsPerPage)     |
| `meta.model.llm`           | String | LLM model used for script generation     |
| `meta.model.image`         | String | Image generation model                   |

## Rate Limiting

-   **Limit**: 60 requests per minute per IP
-   **Window**: 60 seconds
-   **Response**: 429 Too Many Requests

## Examples

### Basic Usage (1 page, 6 panels - default)

#### cURL

```bash
curl -X POST http://localhost:3000/api/comic/generate \
  -F "prompt=Một cô gái phát hiện ra cổng thần bí trong khu rừng, bước qua và gặp sinh vật kỳ lạ"
```

#### JavaScript

```javascript
const form = new FormData();
form.append(
    "prompt",
    "Một cô gái phát hiện ra cổng thần bí trong khu rừng, bước qua và gặp sinh vật kỳ lạ"
);

const response = await fetch("http://localhost:3000/api/comic/generate", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log("Comic image:", result.comic_url);
console.log("Comic ID:", result.data.comic_id);
console.log("Lời thoại panel 1:", result.data.panels[0].dialogue);
```

#### Python

```python
import requests

url = "http://localhost:3000/api/comic/generate"
form = {
    "prompt": (None, "Một cô gái phát hiện ra cổng thần bí trong khu rừng, bước qua và gặp sinh vật kỳ lạ")
}

response = requests.post(url, files=form)

result = response.json()
print(f"Comic image: {result['comic_url']}")
print(f"Total panels: {result['meta']['totalPanels']}")
for panel in result['data']['panels']:
    print(f"Panel {panel['id']}: {panel['speaker']} - {panel['dialogue']}")
```

### Custom Configuration (2 pages, 4 panels each)

#### cURL

```bash
curl -X POST http://localhost:3000/api/comic/generate \
  -F "prompt=Anh hùng đối mặt với quái vật khổng lồ, chiến đấu anh dũng và giành chiến thắng" \
  -F "pages=2" \
  -F "panelsPerPage=4"
```

#### JavaScript

```javascript
const form = new FormData();
form.append(
    "prompt",
    "Anh hùng đối mặt với quái vật khổng lồ, chiến đấu anh dũng và giành chiến thắng"
);
form.append("pages", "2");
form.append("panelsPerPage", "4");

const response = await fetch("http://localhost:3000/api/comic/generate", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(
    `Generated ${result.meta.pages} pages with ${result.meta.panelsPerPage} panels each`
);
```

### Maximum Panels (3 pages, 9 panels each)

#### cURL

```bash
curl -X POST http://localhost:3000/api/comic/generate \
  -F "prompt=Hành trình tìm kiếm kho báu bị mất trong hang động nguy hiểm, gặp nhiều thử thách" \
  -F "pages=3" \
  -F "panelsPerPage=9" \
  -F "style=comic book style art"
```

#### JavaScript

```javascript
const form = new FormData();
form.append(
    "prompt",
    "Hành trình tìm kiếm kho báu bị mất trong hang động nguy hiểm, gặp nhiều thử thách"
);
form.append("pages", "3");
form.append("panelsPerPage", "9");
form.append("style", "comic book style art");

const response = await fetch("http://localhost:3000/api/comic/generate", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(`Total panels: ${result.meta.totalPanels}`); // 27 panels
```

### Node.js Example with File Download

```javascript
import fetch from "node-fetch";
import fs from "fs";

async function generateAndSaveComic() {
    const form = new FormData();
    form.append(
        "prompt",
        "Câu chuyện về một ninja trẻ học võ thuật từ sư phụ già"
    );
    form.append("pages", "1");
    form.append("panelsPerPage", "6");

    const response = await fetch("http://localhost:3000/api/comic/generate", {
        method: "POST",
        body: form,
    });

    const result = await response.json();

    if (result.status === "success") {
        console.log("✅ Comic generated!");
        console.log("Comic URL:", result.comic_url);
        console.log("Script preview:");
        console.log(result.data.script.substring(0, 300) + "...");

        // Download the comic image
        const imageResponse = await fetch(result.data.image.presigned_url);
        const buffer = await imageResponse.arrayBuffer();
        fs.writeFileSync("comic.png", Buffer.from(buffer));
        console.log("💾 Saved to comic.png");
    }
}

generateAndSaveComic();
```

## Processing Pipeline

### 1. Script Generation (Gemini)

Gemini tạo script chi tiết theo format comic book chuyên nghiệp:

-   Phân tích prompt của user
-   Tạo script với cấu trúc: Page → Panel → Description + Dialogue
-   Mỗi panel có: Số thứ tự, mô tả cảnh chi tiết, caption/dialogue
-   Format markdown chuẩn comic script

**Example Script Output:**

```markdown
### Page 1

**Panel 1**  
_Description:_ The cityscape gleams under a bright sun, hovercars gracefully navigating between white buildings adorned with intricate blue tech patterns.  
_Caption:_ The day began like any other in the futuristic city of Neonexus.

**Panel 2**  
_Description:_ People in professional attire hurry along the streets, their eyes fixed on the sky.  
_Caption:_ The citizens of Neonexus moved with purpose, unaware of the impending chaos.

**Panel 3**  
_Description:_ Kai, a young man with a determined expression, rushes towards the towering spire at the heart of the city.  
_Kai:_ (thought bubble) "Something feels off today."

[...]
```

### 2. Prompt Building for Nano Banana

Từ script được tạo, hệ thống xây dựng prompt cho Nano Banana:

```
comic book style art of [FULL SCRIPT], drawing, by Dave Stevens, by Adam Hughes,
1940's, 1950's, hand-drawn, color, high resolution, best quality
```

**Features:**

-   Prefix style có thể tùy chỉnh (default: "comic book style art")
-   Bao gồm toàn bộ script với chi tiết từng panel
-   Quality suffix: drawing style, artist references, era, quality tags

### 3. Image Generation (Nano Banana)

**Model**: `google/nano-banana`

-   Input: Prompt đầy đủ với script
-   Aspect ratio: 2:3 (portrait, phù hợp comic book)
-   Output: Single image với layout tự động nhiều panel

**Nano Banana tự động:**

-   Phân chia layout dựa trên số panel trong script
-   Sắp xếp panels theo thứ tự comic book (top-to-bottom, left-to-right)
-   Thêm speech bubbles và text vào đúng vị trí
-   Tạo comic book styling (borders, gutters, typography)

### 4. Storage & Delivery

-   Image được convert sang PNG
-   Upload lên R2 storage
-   Path: `comics/{comic_id}/comic.png`
-   Presigned URL expires in 1 hour
-   PNG format for best quality

## Processing Time

-   **1 page, 3-6 panels**: 40-90 seconds
-   **2 pages, 6 panels each**: 60-120 seconds
-   **3 pages, 9 panels each**: 90-180 seconds

_Time includes: script generation + prompt building + Nano Banana image generation + upload_

## Panel Configuration

### Recommended Configurations

#### Quick Story (1 page, 3-4 panels)

-   Best for: Simple stories, short sequences
-   Processing time: ~40-60 seconds

#### Standard Comic (1 page, 6 panels) - **Default**

-   Best for: Complete short stories
-   Processing time: ~60-90 seconds

#### Extended Story (2 pages, 4-6 panels each)

-   Best for: Medium-length narratives
-   Processing time: ~80-120 seconds

#### Epic Tale (3 pages, 6-9 panels each)

-   Best for: Complex stories with multiple acts
-   Processing time: ~120-180 seconds

## Prompt Writing Tips

### Excellent Prompts ✅

**Story-focused with clear progression:**

-   "Một phù thủy trẻ khám phá thư viện ma thuật, tìm thấy cuốn sách cổ, triệu hồi rồng và trở thành bạn"
-   "Chiến binh đối đầu với rồng trong hang động lửa, chiến đấu dũng cảm, cuối cùng giành chiến thắng"
-   "Cô gái robot tỉnh dậy, tìm kiếm ký ức bị mất, gặp người tạo ra mình, khám phá sự thật đau lòng"

**Clear setting and character:**

-   "Trong tương lai cyberpunk, hacker trẻ xâm nhập hệ thống tập đoàn, phát hiện âm mưu, phải chạy trốn"
-   "Tại làng ninja ẩn mình, học trò phát hiện sư phụ là kẻ phản bội, phải đối đầu để cứu làng"

**Action and emotion:**

-   "Hai anh em sinh đôi chia tay ở ngã tư đường, gặp lại sau 10 năm, ôm nhau khóc"
-   "Nữ hiệp sĩ bảo vệ làng khỏi quái vật, bị thương nặng nhưng không từ bỏ"

### Prompts to Avoid ❌

**Too vague:**

-   "Một câu chuyện hay" → Không đủ chi tiết
-   "Vẽ ảnh đẹp" → Không có narrative

**Too short:**

-   "Con mèo dễ thương" → Minimum 10 characters, cần story
-   "Phong cảnh đẹp" → Not a story

**Too complex:**

-   Prompts > 500 từ với quá nhiều chi tiết → Gemini sẽ tự tóm tắt
-   Quá nhiều nhân vật (>5) → Khó maintain consistency

## Use Cases

### Entertainment

-   Quick comic stories
-   Visual narratives
-   Fan fiction visualization
-   Story prototypes

### Content Creation

-   Social media comics
-   Blog illustrations
-   Educational comics
-   Tutorial sequences

### Marketing

-   Product story comics
-   Brand narratives
-   Explainer comics
-   Advertisement storyboards

### Creative Writing

-   Story visualization
-   Character development
-   Plot planning
-   Scene composition

## Technical Details

### AI Models

**Gemini 2.5 Flash:**

-   Purpose: Script generation
-   Temperature: 0.3 (balanced creativity/consistency)
-   Max tokens: 8000
-   Output: Structured comic script in markdown

**Google Nano Banana:**

-   Purpose: Comic image generation
-   Aspect ratio: 2:3 (portrait comic book format)
-   Auto-layout: Multiple panels with proper spacing
-   Built-in: Speech bubbles, text, comic styling

### Image Output

-   Format: PNG
-   Aspect ratio: 2:3 (portrait)
-   Size: Optimized by Nano Banana (typically 1024x1536 or similar)
-   Quality: High resolution, suitable for web and print
-   File size: ~2-8MB depending on complexity

### Storage

-   Storage: Cloudflare R2
-   Path pattern: `comics/{comic_id}/comic.png`
-   Presigned URLs: 1 hour expiry
-   Public URLs: Available if R2 public access enabled

## Error Handling

### Automatic Retry

-   Gemini API calls: 2 retries
-   Nano Banana API calls: 2 retries
-   Exponential backoff (600ms base, 2x factor)
-   Image download: 30 second timeout per attempt

### Fallback Behavior

If script generation partially fails:

-   System uses user prompt directly in Nano Banana
-   May result in simpler comic structure

## Best Practices

### Prompt Writing ✅

**DO:**

-   Write story with beginning, middle, end
-   Include character emotions and actions
-   Specify setting and mood
-   Keep prompts 20-200 words
-   Focus on visual storytelling

**DON'T:**

-   Write non-narrative prompts
-   Include NSFW content
-   Expect specific art styles that conflict with comic book format
-   Rely on extremely detailed art direction

### API Usage ✅

**DO:**

-   Cache results using `comic_id`
-   Download and store comic images
-   Use `request_id` for debugging
-   Handle processing times gracefully (40-180 seconds)

**DON'T:**

-   Rely on presigned URLs long-term (1 hour expiry)
-   Request same story multiple times
-   Ignore rate limits

### Configuration Selection ✅

**For short stories** (1-2 min read):

-   `pages: 1, panelsPerPage: 3-6`

**For medium stories** (3-5 min read):

-   `pages: 1-2, panelsPerPage: 6-9`

**For long stories** (5+ min read):

-   `pages: 2-3, panelsPerPage: 6-9`

## Limitations

### Current Limitations

-   Maximum 3 pages per request
-   Maximum 9 panels per page (27 panels total)
-   Aspect ratio locked to 2:3 (portrait)
-   Comic book style only (via Nano Banana)
-   Single image output (not individual panels)

### Content Restrictions

-   NSFW content may be filtered by model
-   Copyrighted characters not guaranteed
-   Text in non-Latin scripts may have quality issues

## Error Codes

| Code                  | HTTP Status | Description              | Solution                                                    |
| --------------------- | ----------- | ------------------------ | ----------------------------------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters | Check prompt length (≥10), pages (1-3), panelsPerPage (3-9) |
| `PROCESSING_ERROR`    | 400         | Comic generation failed  | Retry with different prompt or configuration                |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests        | Wait and retry after cooldown period                        |
| `INTERNAL_ERROR`      | 500         | Server error             | Contact support with `request_id`                           |

## Troubleshooting

### Comic generation timeout

**Symptoms:** Request takes > 3 minutes
**Solutions:**

-   Reduce `pages` count
-   Reduce `panelsPerPage` count
-   Simplify prompt
-   Retry during off-peak hours

### Poor quality results

**Symptoms:** Comic doesn't match story well
**Solutions:**

-   Make prompt more descriptive and story-focused
-   Add more details about characters and setting
-   Specify emotions and actions clearly
-   Try different `panelsPerPage` configuration

### Script too long/complex

**Symptoms:** Generated script is overwhelming
**Solutions:**

-   Reduce `pages` or `panelsPerPage`
-   Simplify user prompt
-   Focus on core story elements

### Image download fails

**Symptoms:** Presigned URL doesn't work
**Solutions:**

-   URLs expire after 1 hour - download immediately
-   Use public URL if available
-   Regenerate if needed

## Support

For issues or questions:

1. Check prompt meets minimum requirements (≥10 characters)
2. Verify configuration is within limits
3. Review error messages and `request_id`
4. Ensure Gemini and Replicate API keys configured
5. Check R2 storage credentials
6. Verify Nano Banana model access

## Changelog

### v2.0.0 (Current - Hybrid Nano Banana + Vietnamese)

-   **NEW**: Hybrid approach - Nano Banana + Vietnamese overlay
-   **NEW**: Lời thoại tiếng Việt tự nhiên từ Gemini
-   **NEW**: SVG speech bubbles tiếng Việt overlay
-   **NEW**: Multi-page support (1-3 pages)
-   **NEW**: Configurable panels per page (3-9)
-   **IMPROVED**: Layout đẹp từ Nano Banana
-   **IMPROVED**: Lời thoại tiếng Việt dễ đọc
-   **IMPROVED**: Font hỗ trợ tiếng Việt (Noto Sans)
-   **PERFECT FOR**: Thị trường Việt Nam 🇻🇳

### v1.0.0 (Legacy - Animagine)

-   Gemini 2.5 Flash for storyboard
-   Animagine XL 3.1 for individual panels
-   Custom panel composition
-   1-6 panels support (single page only)

## Migration from v1.0.0

If you were using the old API:

**Old API:**

```javascript
{
  prompt: "Story here",
  panels: 4,  // ❌ Removed
  style: "anime_color"  // ❌ Changed
}
```

**New API:**

```javascript
{
  prompt: "Story here",
  pages: 1,  // ✅ New
  panelsPerPage: 6,  // ✅ New (similar to old 'panels')
  style: "comic book style art"  // ✅ New default
}
```

**Response changes:**

-   `page_url` → `comic_url`
-   `story_id` → `comic_id`
-   Added: `data.script` (full generated script)
-   Removed: Individual panel data
