# Replace Style API Documentation

## Overview

Service chuyển đổi phong cách ảnh bằng AI sử dụng **FLUX.1 Kontext Pro** model từ Black Forest Labs. Cho phép biến đổi ảnh sang các phong cách nghệ thuật khác nhau trong khi vẫn giữ nguyên composition, đặc điểm khuôn mặt và tư thế.

## Endpoint

```
POST /api/style
```

## Description

Upload ảnh và chọn style để biến đổi ảnh sang phong cách nghệ thuật mong muốn. AI sẽ giữ nguyên nội dung chính của ảnh trong khi thay đổi hoàn toàn phong cách thị giác.

## Model Information

-   **Model**: `black-forest-labs/flux-kontext-pro`
-   **Type**: State-of-the-art text-based image editing
-   **Provider**: Replicate
-   **Capabilities**: Style transfer, object editing, background changes với khả năng hiểu prompt xuất sắc

## Supported Styles

### 1. Anime

Transforms images into anime cel-shaded art style with:

-   Clean black ink outlines
-   Flat 2-3 tone shading
-   Vibrant colors with slight saturation boost
-   Specular highlights in eyes
-   Preserved facial features and composition

### 2. Ghibli

Converts images to Studio Ghibli animation style featuring:

-   Hand-drawn watercolor-like brush strokes
-   Soft natural lighting
-   Subtle film grain texture
-   Warm, earthy tones
-   Painterly quality

### 3. Watercolor

Applies watercolor painting effect with:

-   Fluid transparent washes
-   Soft blended edges
-   Visible paper texture
-   Subtle color granulation
-   Light bloom effects
-   Natural skin tones preservation

### 4. Oil Painting

Creates classical oil painting on canvas with:

-   Visible thick impasto brushwork
-   Rich color depth
-   Soft blended edges
-   Realistic chiaroscuro lighting
-   Traditional Renaissance techniques
-   Preserved skin tones and color palette

### 5. Sketches

Transforms to colored pencil sketch style with:

-   Fine graphite-like hatching
-   Clean precise linework
-   Subtle cross-hatch shading
-   Textured paper appearance
-   Preserved color palette (not grayscale)

### 6. Cartoon

Converts to 1990s animated cartoon style with:

-   Bold outlines
-   Simplified features
-   Vibrant flat colors
-   Classic hand-drawn animation techniques
-   Cel-shading

## Rate Limiting

-   **Limit**: 60 requests per minute per IP
-   **Window**: 60 seconds
-   **Response**: 429 Too Many Requests

```json
{
    "success": false,
    "error": "Too many requests",
    "code": "RATE_LIMIT_EXCEEDED",
    "retryAfter": 42
}
```

## Examples

### Basic Usage - Anime Style

#### cURL

```bash
curl -X POST http://localhost:3000/api/style \
  -F "image=@photo.jpg" \
  -F "style=anime"
```

#### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("style", "anime");

const response = await fetch("http://localhost:3000/api/style", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Styled image:", result.data.url);
```

#### Python

```python
import requests

files = {'image': open('photo.jpg', 'rb')}
data = {'style': 'anime'}

response = requests.post(
    'http://localhost:3000/api/style',
    files=files,
    data=data
)

result = response.json()
print(f"Result: {result['data']['url']}")
```

### Advanced Usage - With Extra Prompt

#### cURL

```bash
curl -X POST http://localhost:3000/api/style \
  -F "image=@portrait.jpg" \
  -F "style=ghibli" \
  -F "extra=add a sunset background with clouds"
```

#### JavaScript

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("style", "watercolor");
formData.append("extra", "make the colors more vibrant");

const response = await fetch("http://localhost:3000/api/style", {
    method: "POST",
    body: formData,
});

const result = await response.json();
```

### Node.js Example

```javascript
import fs from "fs";
import FormData from "form-data";
import fetch from "node-fetch";

const form = new FormData();
form.append("image", fs.createReadStream("./photo.jpg"));
form.append("style", "oil-painting");
form.append("extra", "add dramatic lighting");

const response = await fetch("http://localhost:3000/api/style", {
    method: "POST",
    body: form,
});

const result = await response.json();
console.log(result);
```

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter | Type   | Required | Description                                                      | Default |
| --------- | ------ | -------- | ---------------------------------------------------------------- | ------- |
| `image`   | File   | ✅       | File ảnh (JPEG, PNG, WebP)                                       | -       |
| `style`   | String | ✅       | Phong cách muốn áp dụng (xem danh sách bên dưới)                 | -       |
| `extra`   | String | ❌       | Mô tả bổ sung để tùy chỉnh kết quả (VD: "add sunset background") | -       |

### Constraints

-   **Max file size**: 10MB (theo config upload middleware)
-   **Supported formats**: JPEG, PNG, WebP
-   **Supported styles**: `anime`, `ghibli`, `watercolor`, `oil-painting`, `sketches`, `cartoon`

## Response

### Success Response (200 OK)

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
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### Error Response (400 Bad Request)

```json
{
    "success": false,
    "requestId": "req_abc123",
    "error": {
        "message": "Invalid input",
        "code": "VALIDATION_ERROR",
        "details": "style không hợp lệ. Hỗ trợ: anime, ghibli, watercolor, oil-painting, sketches, cartoon"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### Error Response (500 Internal Server Error)

```json
{
    "success": false,
    "requestId": "req_abc123",
    "error": {
        "message": "Style replacement failed",
        "code": "INTERNAL_ERROR"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

## Response Fields

| Field               | Type    | Description                        |
| ------------------- | ------- | ---------------------------------- |
| `success`           | Boolean | true nếu thành công, false nếu lỗi |
| `requestId`         | String  | Unique request identifier          |
| `data.key`          | String  | R2 storage key                     |
| `data.url`          | String  | Public URL                         |
| `data.presignedUrl` | String  | Presigned URL for secure access    |
| `data.expiresIn`    | Number  | Presigned URL expiration (seconds) |
| `meta.style`        | String  | Style đã áp dụng                   |
| `meta.bytes`        | Number  | Kích thước file output (bytes)     |

## Processing Details

### How FLUX Kontext Pro Works

1. **Image Analysis**: AI phân tích composition, đối tượng và cấu trúc ảnh
2. **Style Understanding**: Hiểu và áp dụng style prompt chi tiết
3. **Feature Preservation**: Giữ nguyên khuôn mặt, tư thế, composition
4. **Style Transfer**: Biến đổi visual style hoàn toàn
5. **Detail Enhancement**: Thêm chi tiết phù hợp với style mới
6. **Output Generation**: Tạo ảnh final với style đã chọn

### Processing Time

-   **Small images** (< 1MP): 30-60 seconds
-   **Medium images** (1-4MP): 60-90 seconds
-   **Large images** (> 4MP): 90-150 seconds

_Note: Processing time phụ thuộc vào Replicate API load và độ phức tạp của ảnh_

### Pre-processing

-   **Pre-scaling**: Ảnh lớn hơn 1024px sẽ được resize xuống
-   **Format Conversion**: Tất cả ảnh converted về JPEG format
-   **Quality**: 92% JPEG quality cho balance giữa size và chất lượng

## Use Cases

### Creative Photography

-   Biến ảnh thành nghệ thuật
-   Tạo avatar/profile pictures độc đáo
-   Social media content
-   Portfolio diversification

### E-commerce

-   Product photos với nhiều styles
-   Marketing materials
-   Brand storytelling
-   Catalog variations

### Content Creation

-   YouTube thumbnails
-   Blog post illustrations
-   Book covers
-   Presentation visuals

### Personal Projects

-   Transform family photos
-   Create artistic prints
-   Gift personalization
-   Memory preservation

### Controller Layer (`replaceStyle.controller.js`)

Handles HTTP request/response:

-   Validates input using schema
-   Calls service layer
-   Generates presigned URLs for secure access
-   Returns formatted response with public and presigned URLs

### Schema Validation (`replaceStyle.schema.js`)

Validates:

-   File presence and type (must be image/\*)
-   Style parameter (must be one of allowed styles)
-   Extra parameter (optional string)

### Routes (`replaceStyle.routes.js`)

Should define:

-   POST endpoint with file upload middleware
-   Rate limiting
-   Timeout handling
-   Error handling

## Technical Notes

### AI Model

-   **Source**: Black Forest Labs
-   **Model**: FLUX.1 Kontext Pro
-   **Platform**: Replicate API
-   **Type**: State-of-the-art text-based image editing
-   **Specialty**: Style transfer với preservation control

### Storage

-   Output images stored in Cloudflare R2
-   Automatic key generation
-   Prefix: `styles/{style}/`
-   Format: JPEG (optimized 92% quality)

### Concurrency

-   Uses Replicate limiter to prevent API overload
-   Retry logic: 2 retries with exponential backoff (800ms base)
-   Request queuing during high load

### Performance Optimizations

1. **Image Pre-scaling**: Reduces large images to max 1024px
2. **Replicate Limiter**: Prevents too many concurrent API calls
3. **Retry Logic**: Automatic retry on transient failures
4. **R2 Storage**: Fast CDN delivery with presigned URLs
5. **Quality Setting**: 92% JPEG quality for optimal size/quality balance

## Prompting Best Practices

The service follows FLUX Kontext Pro best practices:

### ✅ Do:

-   Use specific, detailed language with exact colors and descriptions
-   Use action verbs like "Change" for controlled transformations
-   Explicitly state what to preserve: "Keep the exact same facial features"
-   Reference known art movements and styles
-   Describe key traits: "visible brushstrokes, thick paint texture"

### ❌ Don't:

-   Use vague terms like "make it better" or "transform"
-   Forget to specify preservation requirements
-   Use generic style descriptions
-   Expect complex multi-step edits in single prompt

## Best Practices

### Image Input

✅ **DO:**

-   Use clear, well-lit photos
-   Ensure subjects are in focus
-   Use decent resolution (512x512 minimum)
-   Test different styles on same image
-   Pre-resize very large images client-side

❌ **DON'T:**

-   Upload extremely low-res images (< 256px)
-   Use heavily compressed/artifacted photos
-   Expect perfect results from blurry images
-   Process same image repeatedly with same style

### Extra Prompt Usage

✅ **DO:**

-   Be specific: "add golden sunset background"
-   Use descriptive language: "make colors more vibrant and saturated"
-   Reference art techniques: "add visible brushstrokes"
-   Keep prompts under 50 words

❌ **DON'T:**

-   Write vague prompts: "make it better"
-   Contradict the style: "anime style but realistic"
-   Write very long prompts (> 100 words)
-   Request multiple unrelated changes

### API Usage

✅ **DO:**

-   Implement retry logic with exponential backoff
-   Handle rate limits gracefully
-   Cache results using storage keys
-   Use request_id for debugging
-   Download and store important results

❌ **DON'T:**

-   Process same image multiple times unnecessarily
-   Ignore error responses
-   Store presigned URLs long-term (they expire!)
-   Skip client-side validation

## Error Codes

| Code                  | HTTP Status | Description                | Solution                            |
| --------------------- | ----------- | -------------------------- | ----------------------------------- |
| `VALIDATION_ERROR`    | 400         | Invalid input parameters   | Check file format and style value   |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry after specified time |
| `INTERNAL_ERROR`      | 500         | Server processing error    | Retry or contact support            |
| `REPLICATE_ERROR`     | 500         | AI model processing failed | Try different image or retry later  |

## Style Selection Guide

### When to Use Each Style

**Anime**

-   Best for: Portraits, characters, fan art
-   Characteristics: Bold lines, vibrant colors
-   Works well with: Clear subjects, good lighting

**Ghibli**

-   Best for: Landscapes, nostalgic scenes, nature
-   Characteristics: Soft, painterly, warm tones
-   Works well with: Outdoor photos, natural settings

**Watercolor**

-   Best for: Artistic portraits, gentle scenes
-   Characteristics: Soft edges, transparent washes
-   Works well with: Well-lit photos, simple compositions

**Oil Painting**

-   Best for: Classical portraits, fine art
-   Characteristics: Rich colors, visible brushwork
-   Works well with: Portrait photos, dramatic lighting

**Sketches**

-   Best for: Quick artistic renditions
-   Characteristics: Line art with subtle color
-   Works well with: Any clear subject

**Cartoon**

-   Best for: Fun, playful transformations
-   Characteristics: Bold outlines, simplified features
-   Works well with: Expressive photos, clear subjects

## Troubleshooting

### Style not applied correctly

-   Check if image quality is sufficient
-   Try without `extra` parameter first
-   Ensure style name is spelled correctly
-   Try different style for your image type

### Output looks distorted

-   Reduce input image size before upload
-   Check if original image is clear and in focus
-   Avoid heavily edited/filtered inputs
-   Try simpler composition images

### Processing timeout

-   Reduce image size before upload
-   Retry during off-peak hours
-   Check Replicate API status
-   Contact support if persists

### Colors look unnatural

-   Try different style (each has different color handling)
-   Use `extra` to guide color: "keep natural skin tones"
-   Check input image color quality

## Comparison with Other Services

| Feature           | Replace Style | AI Beautify | Comic Generate |
| ----------------- | ------------- | ----------- | -------------- |
| Style variety     | ✅ 6 styles   | ❌ Fixed    | ✅ Anime only  |
| Preserve features | ✅ Excellent  | ✅ Good     | ⚠️ Transform   |
| Custom prompts    | ✅ Yes        | ❌ No       | ✅ Yes         |
| Processing speed  | ⚡ Medium     | ⚡ Fast     | ⚠️ Slower      |
| Best for          | Art styles    | Enhancement | Comics/Stories |

## Commercial Use

FLUX.1 Kontext Pro outputs có thể sử dụng thương mại trong apps, marketing, hoặc bất kỳ mục đích kinh doanh nào mà không có hạn chế.

## Support

For issues or questions:

1. Check style name spelling and supported values
2. Review error messages and request_id
3. Verify input image quality and format
4. Test with different images/styles
5. Check Replicate API documentation
6. Ensure Replicate API token is configured
7. Verify R2 storage credentials

## Dependencies

### External Services

-   **Replicate API**: FLUX Kontext Pro model
-   **Cloudflare R2**: Image storage

### npm Packages

-   `sharp`: Image processing
-   `replicate`: Replicate API client
-   `zod`: Schema validation

## Changelog

### v1.0.0 (Current)

-   FLUX.1 Kontext Pro integration
-   6 preset styles
-   Custom extra prompt support
-   R2 storage integration
-   Presigned URL generation
-   Rate limiting (60 req/min)
-   Auto pre-scaling (max 1024px)
-   Retry logic with exponential backoff

---

**Last Updated**: November 18, 2025  
**Model Version**: flux-kontext-pro  
**API Version**: v1
