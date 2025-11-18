# Comic Generation API Documentation

## Overview

Service tạo truyện tranh anime tự động từ prompt văn bản. Hệ thống sử dụng kết hợp 2 AI models:

1. **Gemini 2.5 Flash** - Tạo storyboard (kịch bản, thoại, mô tả cảnh)
2. **Animagine XL 3.1** - Sinh ảnh anime theo từng panel

Kết quả là một trang truyện comic hoàn chỉnh với layout chuyên nghiệp, speech bubbles và thoại tiếng Việt.

## Endpoint

```
POST /api/comic/generate
```

## Description

Tạo một trang comic anime từ prompt văn bản. AI sẽ tự động tạo storyboard, sinh hình ảnh cho từng panel, và composite thành trang comic hoàn chỉnh với thoại.

## Request

### Headers

```
Content-Type: application/json
```

### Body Parameters

| Parameter | Type   | Required | Description                  | Default         |
| --------- | ------ | -------- | ---------------------------- | --------------- |
| `prompt`  | String | ✅       | Mô tả câu chuyện (≥ 5 ký tự) | -               |
| `panels`  | Number | ❌       | Số lượng panel (1-6)         | `4`             |
| `style`   | String | ❌       | Style của comic              | `"anime_color"` |

### Constraints

-   **Prompt length**: Tối thiểu 5 ký tự
-   **Panels**: 1-6 panels per page
-   **Style**: Hiện tại hỗ trợ "anime_color"

## Response

### Success Response (200 OK)

```json
{
    "request_id": "req_abc123xyz",
    "status": "success",
    "page_url": "https://pub-xxxx.r2.dev/comics/story-id/page-0.png",
    "data": {
        "key": "comics/550e8400-e29b-41d4-a716-446655440000/page-0.png",
        "url": "https://pub-xxxx.r2.dev/comics/.../page-0.png",
        "presigned_url": "https://pub-xxxx.r2.dev/comics/...?X-Amz-Algorithm=..."
    },
    "meta": {
        "story_id": "550e8400-e29b-41d4-a716-446655440000",
        "panels": [
            {
                "id": 1,
                "dialogue": "Xin chào! Tôi là nhân vật chính.",
                "speaker": "Hero",
                "emotion": "happy"
            },
            {
                "id": 2,
                "dialogue": "Cuộc phiêu lưu bắt đầu thôi!",
                "speaker": "Hero",
                "emotion": "excited"
            }
        ],
        "model": {
            "llm": "google/gemini-2.5-flash",
            "image": "cjwbw/animagine-xl-3.1"
        }
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
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
            "prompt": "prompt too short"
        }
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### Error Response (500 Internal Server Error)

```json
{
    "request_id": "req_abc123xyz",
    "status": "error",
    "error": {
        "message": "Failed to generate comic",
        "code": "PROCESSING_ERROR"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

## Response Fields

| Field                | Type   | Description                                    |
| -------------------- | ------ | ---------------------------------------------- |
| `request_id`         | String | Unique request identifier                      |
| `status`             | String | "success" or "error"                           |
| `page_url`           | String | Direct URL to comic page                       |
| `data.key`           | String | R2 storage key                                 |
| `data.url`           | String | Public URL                                     |
| `data.presigned_url` | String | Presigned URL (expires in 1 hour)              |
| `meta.story_id`      | String | Unique story identifier                        |
| `meta.panels`        | Array  | Panel information (dialogue, speaker, emotion) |
| `meta.model.llm`     | String | LLM model used for storyboard                  |
| `meta.model.image`   | String | Image generation model                         |

## Rate Limiting

-   **Limit**: 60 requests per minute per IP
-   **Window**: 60 seconds
-   **Response**: 429 Too Many Requests

## Examples

### Basic Usage (4 panels)

#### cURL

```bash
curl -X POST http://localhost:3000/api/comic/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Một cô gái phát hiện ra cổng thần bí trong khu rừng"
  }'
```

#### JavaScript (Fetch)

```javascript
const response = await fetch("http://localhost:3000/api/comic/generate", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
    },
    body: JSON.stringify({
        prompt: "Một cô gái phát hiện ra cổng thần bí trong khu rừng",
    }),
});

const result = await response.json();
console.log("Comic page:", result.page_url);
console.log("Story ID:", result.meta.story_id);
```

#### Python

```python
import requests

response = requests.post(
    'http://localhost:3000/api/comic/generate',
    json={
        'prompt': 'Một cô gái phát hiện ra cổng thần bí trong khu rừng'
    }
)

result = response.json()
print(f"Comic page: {result['page_url']}")
print(f"Panels: {len(result['meta']['panels'])}")
```

### Custom Panels (2 panels)

#### cURL

```bash
curl -X POST http://localhost:3000/api/comic/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Anh hùng đối mặt với quái vật khổng lồ",
    "panels": 2
  }'
```

#### JavaScript

```javascript
const response = await fetch("http://localhost:3000/api/comic/generate", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
    },
    body: JSON.stringify({
        prompt: "Anh hùng đối mặt với quái vật khổng lồ",
        panels: 2,
    }),
});

const result = await response.json();
```

### Maximum Panels (6 panels)

#### cURL

```bash
curl -X POST http://localhost:3000/api/comic/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Hành trình tìm kiếm kho báu bị mất trong hang động",
    "panels": 6,
    "style": "anime_color"
  }'
```

#### JavaScript

```javascript
const response = await fetch("http://localhost:3000/api/comic/generate", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
    },
    body: JSON.stringify({
        prompt: "Hành trình tìm kiếm kho báu bị mất trong hang động",
        panels: 6,
        style: "anime_color",
    }),
});

const result = await response.json();

// Display all panel dialogues
result.meta.panels.forEach((panel) => {
    console.log(`Panel ${panel.id}: ${panel.speaker} - ${panel.dialogue}`);
});
```

### Node.js Example

```javascript
import fetch from "node-fetch";

async function generateComic() {
    const response = await fetch("http://localhost:3000/api/comic/generate", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            prompt: "Câu chuyện về một ninja trẻ học võ thuật",
            panels: 4,
        }),
    });

    const result = await response.json();

    if (result.status === "success") {
        console.log("✅ Comic generated!");
        console.log("Page URL:", result.page_url);
        console.log("Story ID:", result.meta.story_id);

        // Download the comic page
        const imageResponse = await fetch(result.data.presigned_url);
        const buffer = await imageResponse.arrayBuffer();
        // Save or process buffer...
    }
}

generateComic();
```

## Processing Pipeline

### 1. Storyboard Generation (Gemini)

-   Phân tích prompt của user
-   Tạo storyboard với số lượng panels yêu cầu
-   Sinh thoại tiếng Việt tự nhiên (≤ 40 ký tự)
-   Tạo prompt tags chi tiết cho từng panel
-   Xác định nhân vật, cảm xúc, góc máy

**Output:**

```json
{
    "story_id": "uuid",
    "characters": [
        {
            "name": "Hero",
            "role": "main",
            "description_en": "young ninja, blue eyes"
        }
    ],
    "panels": [
        {
            "id": 1,
            "description_vi": "Ninja trẻ đứng trên núi cao, ánh sáng hoàng hôn",
            "prompt_tags": "masterpiece, anime style, young ninja, mountain top, sunset lighting",
            "dialogue": "Hành trình của tôi mới bắt đầu...",
            "speaker": "Hero",
            "emotion": "determined"
        }
    ]
}
```

### 2. Image Generation (Animagine)

**For each panel:**

-   Generate image từ prompt_tags
-   Resolution: 832x1216 (portrait ratio)
-   28 inference steps
-   Guidance scale: 7
-   Negative prompts để tránh NSFW, text, watermarks

**Quality settings:**

-   Vibrant anime colors
-   Detailed backgrounds
-   No text/speech bubbles in images
-   High quality anime style

### 3. Page Composition

**Layout system:**

-   **1 panel**: Full page
-   **2 panels**: Side by side
-   **3-6 panels**: Grid layout
-   Page size: 1080x1620px
-   Gap between panels: 18px

**Speech bubbles:**

-   Auto-generated from dialogue
-   White bubbles with black border
-   Positioned top-left of each panel
-   Speaker name included
-   Text wrapping for long dialogue
-   Max 40 characters per dialogue

### 4. Storage & Delivery

-   Composite page uploaded to R2
-   Path: `comics/{story_id}/page-0.png`
-   Presigned URL expires in 1 hour
-   PNG format for quality

## Processing Time

-   **2 panels**: 60-90 seconds
-   **4 panels**: 90-150 seconds
-   **6 panels**: 150-240 seconds

_Time includes: storyboard generation + image generation for all panels + composition_

## Panel Configuration

### 1 Panel

-   Best for: Title pages, splash pages
-   Layout: Full page single image

### 2 Panels

-   Best for: Before/after, dialogue scenes
-   Layout: Side by side

### 3 Panels

-   Best for: Quick action sequences
-   Layout: Grid (usually 1x3 or 2x2)

### 4 Panels (Recommended)

-   Best for: Standard storytelling
-   Layout: 2x2 grid
-   Balanced composition

### 5 Panels

-   Best for: Detailed sequences
-   Layout: Mixed grid

### 6 Panels (Maximum)

-   Best for: Complex stories
-   Layout: 2x3 or 3x2 grid
-   More detailed narrative

## Prompt Writing Tips

### Good Prompts

✅ **Story-focused:**

-   "Một phù thủy trẻ khám phá thư viện ma thuật"
-   "Chiến binh đối đầu với rồng trong hang động lửa"
-   "Cô gái robot tìm kiếm ký ức bị mất"

✅ **Clear setting:**

-   "Trong tương lai cyberpunk, hacker trẻ..."
-   "Tại làng ninja ẩn mình, học trò phát hiện..."

✅ **Character-driven:**

-   "Hai anh em sinh đôi chia tay ở ngã tư đường"
-   "Nữ hiệp sĩ bảo vệ làng khỏi quái vật"

### Avoid

❌ **Too vague:**

-   "Một câu chuyện hay"
-   "Vẽ ảnh anime"

❌ **Too complex:**

-   Prompts > 200 từ với quá nhiều chi tiết
-   Quá nhiều nhân vật (tốt nhất 1-3 nhân vật)

❌ **Not story-like:**

-   "Con mèo dễ thương"
-   "Phong cảnh đẹp"

## Use Cases

### Storytelling

-   Quick comic stories
-   Visual narratives
-   Character introductions
-   Story prototypes

### Content Creation

-   Social media content
-   Blog illustrations
-   Educational comics
-   Tutorial sequences

### Entertainment

-   Short comic strips
-   Meme comics
-   Fan fiction visualization
-   Creative writing aids

### Marketing

-   Product story comics
-   Brand narratives
-   Explainer comics
-   Advertisement storyboards

## Technical Details

### AI Models

**Gemini 2.5 Flash:**

-   Purpose: Storyboard generation
-   Temperature: 0.25 (more consistent)
-   Max tokens: 5000
-   Output: Structured JSON

**Animagine XL 3.1:**

-   Purpose: Anime image generation
-   Steps: 28
-   Guidance: 7
-   Resolution: 832x1216
-   Negative prompts: NSFW, text, logos, watermarks

### Layout Engine

-   Smart grid layout based on panel count
-   Maintains aspect ratios
-   Equal spacing with 18px gaps
-   Responsive panel sizing

### Speech Bubbles

-   SVG-based rendering
-   Auto text wrapping
-   Max line length: calculated dynamically
-   Font: Arial (web-safe)
-   Font size: 18px

### Storage

-   Format: PNG
-   Composite page size: ~2-5MB
-   Storage path: `comics/{story_id}/page-0.png`
-   R2 presigned URLs (1 hour expiry)

## Error Handling

### Automatic Retry

-   Replicate API calls: 2 retries
-   Exponential backoff (600ms base)
-   Factor: 2x

### Fallback Storyboard

If Gemini fails to return valid JSON:

-   System generates basic storyboard
-   Uses user prompt for all panels
-   Generic dialogue placeholders
-   Continues with image generation

### Image Fetch Timeout

-   Default: 20 seconds per image
-   Configurable via `HTTP_TIMEOUT_MS`

## Best Practices

### Prompt Writing

✅ **DO:**

-   Write in Vietnamese for natural dialogue
-   Focus on story/action
-   Specify setting/mood
-   Keep prompts 10-100 words
-   Include character descriptions

❌ **DON'T:**

-   Write extremely long prompts
-   Include too many characters (>3)
-   Request specific art styles contradicting anime
-   Include NSFW content

### Panel Selection

-   **2 panels**: Simple dialogues
-   **4 panels**: Standard stories (recommended)
-   **6 panels**: Complex narratives

### API Usage

✅ **DO:**

-   Cache results using story_id
-   Download and store comic pages
-   Use request_id for debugging
-   Handle long processing times (2-4 minutes)

❌ **DON'T:**

-   Rely on presigned URLs long-term
-   Request same story multiple times
-   Ignore processing time warnings

## Limitations

### Current Limitations

-   Single page output only
-   Maximum 6 panels per page
-   Vietnamese dialogue only (storyboard)
-   Anime style only
-   No custom character persistence across requests

### Content Restrictions

-   NSFW content filtered
-   No text/logos in generated images
-   No copyrighted characters (may vary)

## Error Codes

| Code                  | HTTP Status | Description              | Solution                                  |
| --------------------- | ----------- | ------------------------ | ----------------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters | Check prompt length (≥5) and panels (1-6) |
| `PROCESSING_ERROR`    | 400         | Comic generation failed  | Retry with different prompt               |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests        | Wait and retry                            |
| `INTERNAL_ERROR`      | 500         | Server error             | Contact support with request_id           |

## Troubleshooting

### Comic generation timeout

-   Reduce number of panels
-   Simplify prompt
-   Retry during off-peak hours

### Poor quality results

-   Make prompt more specific
-   Add setting/mood details
-   Try different panel counts

### Dialogue too long/cut off

-   Keep prompts concise
-   Let AI generate dialogue (don't force long text)
-   System auto-wraps at 40 chars

### Images don't match story

-   Make prompt more descriptive
-   Include character details
-   Specify setting clearly

## Support

For issues or questions:

1. Check prompt meets minimum requirements
2. Verify panel count is 1-6
3. Review error messages and request_id
4. Ensure Gemini and Animagine API keys configured
5. Check R2 storage credentials

## Changelog

### v1.0.0 (Current)

-   Gemini 2.5 Flash for storyboard
-   Animagine XL 3.1 for image generation
-   1-6 panels support
-   Vietnamese dialogue support
-   Auto speech bubbles
-   Smart grid layout
-   R2 storage integration
-   Presigned URL delivery
-   Rate limiting (60 req/min)
