# IC-Light Portrait Relighting API Documentation

## Overview

IC-Light là AI model chuyên về text-guided relighting cho ảnh chân dung. Service này cho phép:

-   Thay đổi ánh sáng của ảnh chân dung theo mô tả văn bản
-   Điều chỉnh hướng nguồn sáng (trái, phải, trên, dưới)
-   Tạo nhiều variations từ một ảnh
-   Kiểm soát chất lượng và kích thước output

## Endpoint

```
POST /api/portraits/ic-light
```

## Description

Upload ảnh chân dung hoặc cung cấp URL, kèm prompt mô tả ánh sáng mong muốn, và nhận về ảnh đã được relight bằng AI.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Body Parameters

| Parameter          | Type   | Required | Description                           | Default                                                    |
| ------------------ | ------ | -------- | ------------------------------------- | ---------------------------------------------------------- |
| `image`            | File   | ❌\*     | File ảnh (JPEG, PNG, WebP)            | -                                                          |
| `image_url`        | String | ❌\*     | URL của ảnh (public hoặc presigned)   | -                                                          |
| `prompt`           | String | ✅       | Mô tả ánh sáng mong muốn              | `"studio soft light, flattering portrait lighting"`        |
| `appended_prompt`  | String | ❌       | Text thêm vào cuối prompt             | `"best quality"`                                           |
| `negative_prompt`  | String | ❌       | Mô tả những gì muốn tránh             | `"lowres, bad anatomy, bad hands, cropped, worst quality"` |
| `light_source`     | String | ❌       | Hướng nguồn sáng (xem bảng dưới)      | `"None"`                                                   |
| `steps`            | Number | ❌       | Số steps inference (1-100)            | `25`                                                       |
| `cfg`              | Number | ❌       | Classifier-free guidance scale (1-32) | `2`                                                        |
| `width`            | Number | ❌       | Chiều rộng output (256-1024, step 64) | Auto                                                       |
| `height`           | Number | ❌       | Chiều cao output (256-1024, step 64)  | Auto                                                       |
| `number_of_images` | Number | ❌       | Số lượng ảnh output (1-12)            | `1`                                                        |
| `output_format`    | String | ❌       | Format output: "webp", "jpg", "png"   | `"webp"`                                                   |
| `output_quality`   | Number | ❌       | Chất lượng output (1-100)             | `80`                                                       |

**Note**: Phải cung cấp `image` HOẶC `image_url` (ít nhất một trong hai)

### Light Source Options

| Value            | Description                  |
| ---------------- | ---------------------------- |
| `"None"`         | Không thêm directional light |
| `"Left Light"`   | Nguồn sáng từ bên trái       |
| `"Right Light"`  | Nguồn sáng từ bên phải       |
| `"Top Light"`    | Nguồn sáng từ trên           |
| `"Bottom Light"` | Nguồn sáng từ dưới           |

## Response

### Success Response (200 OK)

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
        "code": "PROCESSING_ERROR",
        "details": "prompt is required"
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
        "message": "IC-Light processing failed",
        "code": "INTERNAL_ERROR"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

## Response Fields

| Field                  | Type   | Description                     |
| ---------------------- | ------ | ------------------------------- |
| `request_id`           | String | Unique request identifier       |
| `status`               | String | "success" or "error"            |
| `data.outputs`         | Array  | List of output images           |
| `data.outputs[].url`   | String | URL của ảnh output              |
| `data.outputs[].index` | Number | Index của ảnh trong batch       |
| `meta.model`           | String | AI model sử dụng                |
| `meta.prompt`          | String | Full prompt (prompt + appended) |
| `meta.light_source`    | String | Hướng nguồn sáng đã áp dụng     |
| `meta.dimensions`      | Object | Kích thước output               |

## Rate Limiting

-   **Limit**: 60 requests per minute per IP
-   **Window**: 60 seconds
-   **Response**: 429 Too Many Requests

## Examples

### Basic Usage - Upload File

#### cURL

```bash
curl -X POST http://localhost:3000/api/portraits/ic-light \
  -F "image=@./portrait.jpg" \
  -F "prompt=golden hour sunlight, warm tone" \
  -F "light_source=Left Light"
```

#### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("prompt", "golden hour sunlight, warm tone");
formData.append("light_source", "Left Light");
formData.append("number_of_images", "2");

const response = await fetch("http://localhost:3000/api/portraits/ic-light", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log("Relit images:", result.data.outputs);
```

#### Python

```python
import requests

files = {'image': open('portrait.jpg', 'rb')}
data = {
    'prompt': 'golden hour sunlight, warm tone',
    'light_source': 'Left Light',
    'number_of_images': '2'
}

response = requests.post(
    'http://localhost:3000/api/portraits/ic-light',
    files=files,
    data=data
)

result = response.json()
for output in result['data']['outputs']:
    print(f"Image {output['index']}: {output['url']}")
```

### Advanced Usage - Image URL with Custom Settings

#### cURL

```bash
curl -X POST http://localhost:3000/api/portraits/ic-light \
  -F "image_url=https://example.com/portrait.jpg" \
  -F "prompt=dramatic studio lighting, high contrast" \
  -F "light_source=Top Light" \
  -F "steps=40" \
  -F "cfg=3.5" \
  -F "width=768" \
  -F "height=1024" \
  -F "number_of_images=4" \
  -F "output_format=png" \
  -F "output_quality=95"
```

#### JavaScript

```javascript
const formData = new FormData();
formData.append("image_url", "https://example.com/portrait.jpg");
formData.append("prompt", "dramatic studio lighting, high contrast");
formData.append("light_source", "Top Light");
formData.append("steps", "40");
formData.append("cfg", "3.5");
formData.append("width", "768");
formData.append("height", "1024");
formData.append("number_of_images", "4");
formData.append("output_format", "png");
formData.append("output_quality", "95");

const response = await fetch("http://localhost:3000/api/portraits/ic-light", {
    method: "POST",
    body: formData,
});

const result = await response.json();
```

### Multiple Variations

```javascript
// Generate 4 variations with different lighting
const formData = new FormData();
formData.append("image", fileInput.files[0]);
formData.append("prompt", "soft diffused lighting, professional headshot");
formData.append("number_of_images", "4");
formData.append("output_format", "webp");

const response = await fetch("http://localhost:3000/api/portraits/ic-light", {
    method: "POST",
    body: formData,
});

const result = await response.json();

// Display all variations
result.data.outputs.forEach((output, i) => {
    console.log(`Variation ${i + 1}: ${output.url}`);
});
```

## Processing Details

### How IC-Light Works

1. **Image Analysis**: Analyzes the input portrait
2. **Lighting Detection**: Detects current lighting conditions
3. **Text Guidance**: Interprets the prompt for desired lighting
4. **ControlNet**: Uses ControlNet for structure preservation
5. **Relighting**: Applies new lighting while keeping subject intact
6. **Output Generation**: Generates requested number of variations

### Processing Time

-   **Single image**: 30-60 seconds
-   **Multiple images** (2-4): 60-120 seconds
-   **High steps** (>40): Add 20-40 seconds

_Note: Time varies based on Replicate API load_

## Parameters Guide

### Prompt Tips

✅ **Good Prompts:**

-   "golden hour sunlight, warm glow"
-   "studio soft light, flattering portrait lighting"
-   "dramatic rim lighting, moody atmosphere"
-   "natural window light, soft shadows"
-   "neon lights, cyberpunk style"

❌ **Avoid:**

-   Too vague: "nice lighting"
-   Conflicting: "dark shadows, bright light"
-   Non-lighting terms: "make her smile"

### Steps

-   **Low (10-20)**: Faster, less refined
-   **Medium (25-35)**: Balanced (recommended)
-   **High (40-100)**: More refined, slower

### CFG (Classifier-Free Guidance)

-   **Low (1-2)**: More creative, may drift from prompt
-   **Medium (2-4)**: Balanced (recommended)
-   **High (5-32)**: Strict adherence to prompt, may be less natural

### Dimensions

-   Must be multiples of 64
-   Recommended: 768x1024 (portrait) or 1024x768 (landscape)
-   Keep aspect ratio close to original for best results

## Use Cases

### Professional Photography

-   Re-light portrait shoots
-   Fix poorly lit photos
-   Create multiple lighting variations
-   Studio lighting simulation

### Social Media

-   Enhance profile pictures
-   Create aesthetic variations
-   Fix selfie lighting
-   Instagram-ready portraits

### E-commerce

-   Product photos with models
-   Consistent lighting across catalog
-   Fix mixed lighting in photos

### Creative Projects

-   Artistic lighting effects
-   Mood transformation
-   Before/after comparisons
-   Style exploration

## Best Practices

### Image Input

✅ **DO:**

-   Use clear portrait photos
-   Ensure face is visible and well-framed
-   Use decent resolution (512x512 minimum)
-   Center the subject
-   Use frontal or 3/4 view portraits

❌ **DON'T:**

-   Use group photos (works best with single subject)
-   Upload extremely low resolution
-   Use heavily edited/filtered images
-   Expect good results from profile/side views

### Prompt Writing

✅ **DO:**

-   Be specific about lighting type
-   Mention mood/atmosphere
-   Include quality descriptors
-   Test multiple prompts

❌ **DON'T:**

-   Write very long prompts (keep under 100 words)
-   Mix conflicting lighting descriptions
-   Include non-lighting requests

### Parameter Selection

-   **For speed**: steps=20, number_of_images=1
-   **For quality**: steps=40, cfg=3
-   **For exploration**: number_of_images=4, cfg=2
-   **For production**: output_format=png, output_quality=95

## Technical Notes

### AI Model

-   **Base Model**: ControlNet with IC-Light
-   **Platform**: Replicate API
-   **Specialty**: Text-guided relighting for portraits

### Output URLs

-   URLs are hosted on Replicate's delivery CDN
-   URLs are temporary (expire after some time)
-   Download and store images if needed for long-term

### Image URL Support

-   Accepts public URLs
-   Accepts presigned URLs from R2
-   URL must be accessible during processing

## Error Codes

| Code                  | HTTP Status | Description                | Solution                                     |
| --------------------- | ----------- | -------------------------- | -------------------------------------------- |
| `PROCESSING_ERROR`    | 400         | Invalid parameters         | Check prompt, light_source, and other params |
| `VALIDATION_ERROR`    | 400         | Missing required fields    | Ensure prompt and image/image_url provided   |
| `RATE_LIMIT_EXCEEDED` | 429         | Too many requests          | Wait and retry                               |
| `INTERNAL_ERROR`      | 500         | Server/AI processing error | Retry or try different image                 |

## Troubleshooting

### Output doesn't match prompt

-   Increase `cfg` for stricter adherence
-   Increase `steps` for better quality
-   Make prompt more specific
-   Try different `light_source`

### Unnatural results

-   Decrease `cfg` to 2 or less
-   Use simpler prompts
-   Check if input image is suitable
-   Try without `light_source` first

### Faces look distorted

-   Check input image quality
-   Ensure face is clearly visible
-   Try lower `steps` value
-   Use frontal view portraits

### Processing timeout

-   Reduce `number_of_images`
-   Lower `steps` value
-   Use smaller dimensions
-   Retry during off-peak hours

## Comparison with Other Services

| Feature          | IC-Light   | GFPGAN      | AI Beautify |
| ---------------- | ---------- | ----------- | ----------- |
| Lighting control | ✅ Best    | ❌ No       | ⚠️ Limited  |
| Face restoration | ⚠️ Basic   | ✅ Best     | ✅ Good     |
| Multiple outputs | ✅ Yes     | ❌ No       | ❌ No       |
| Text-guided      | ✅ Yes     | ❌ No       | ❌ No       |
| Best for         | Relighting | Restoration | Enhancement |

## Support

For issues or questions:

1. Verify prompt is descriptive and specific
2. Check image quality and format
3. Review error messages and request IDs
4. Test with different parameters
5. Ensure Replicate API key is configured

## Changelog

### v1.0.0 (Current)

-   IC-Light model integration
-   Support for file upload and URL input
-   Configurable lighting direction
-   Multiple output generation (1-12 images)
-   Format and quality options
-   Rate limiting (60 req/min)
