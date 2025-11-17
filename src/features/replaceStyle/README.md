# Replace Style Feature

## Overview

This feature provides AI-powered image style transfer using the **FLUX.1 Kontext Pro** model from Black Forest Labs via Replicate. It allows users to transform images into different artistic styles while preserving the original composition, facial features, and pose.

## Model Information

-   **Model**: `black-forest-labs/flux-kontext-pro`
-   **Type**: State-of-the-art text-based image editing
-   **Provider**: Replicate
-   **Capabilities**: Style transfer, object editing, background changes with excellent prompt following and consistent results

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

## API Endpoint

### POST `/api/replace-style`

**Request Format:**

-   Content-Type: `multipart/form-data`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `image` | File | Yes | Input image file (JPEG, PNG) |
| `style` | String | Yes | Style to apply. Options: `anime`, `ghibli`, `watercolor`, `oil-painting`, `sketches`, `cartoon` |
| `extra` | String | No | Additional prompt details to customize the transformation |

**Example Request:**

```bash
curl -X POST http://localhost:3000/api/replace-style \
  -F "image=@photo.jpg" \
  -F "style=anime" \
  -F "extra=add a sunset background"
```

**Success Response (200):**

```json
{
    "success": true,
    "requestId": "abc123",
    "data": {
        "key": "styles/anime/abc123.jpg",
        "url": "https://your-r2-domain.com/styles/anime/abc123.jpg",
        "presignedUrl": "https://...",
        "expiresIn": 3600,
        "meta": {
            "style": "anime",
            "bytes": 245678,
            "requestId": "abc123"
        }
    }
}
```

**Error Response (400):**

```json
{
    "success": false,
    "requestId": "abc123",
    "error": "Invalid input",
    "code": "VALIDATION_ERROR",
    "details": "style không hợp lệ. Hỗ trợ: anime, ghibli, watercolor, oil-painting, sketches, cartoon"
}
```

## Implementation Details

### Service Layer (`replaceStyle.service.js`)

**Key Functions:**

#### `preScale(buffer)`

-   Optimizes image size before processing
-   Maximum dimension: 1024px (configurable via `PERF.image.maxSidePx`)
-   Reduces API costs and latency
-   Maintains aspect ratio

#### `buildPrompt(style, extra)`

-   Combines preset style prompts with user customizations
-   Follows FLUX Kontext Pro best practices:
    -   Uses specific, descriptive action verbs ("Change" instead of vague "Transform")
    -   Explicitly preserves facial features and composition
    -   Includes detailed style descriptions
    -   Specifies what should remain unchanged

#### `readReplicateOutputToBuffer(out)`

-   Handles both FileOutput (SDK) and URL string formats
-   Downloads and converts result to Buffer
-   Compatible with Replicate SDK updates

#### `applyStyle({ inputBuffer, inputMime, style, extra, requestId })`

Main service function that:

1. Pre-scales the input image
2. Builds the prompt
3. Calls Replicate API with rate limiting
4. Retries on failure (2 retries with exponential backoff)
5. Uploads result to R2 storage
6. Returns storage key and metadata

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

## Performance Optimizations

1. **Image Pre-scaling**: Reduces large images to max 1024px
2. **Replicate Limiter**: Prevents too many concurrent API calls
3. **Retry Logic**: Automatic retry on transient failures (2 retries, 800ms base delay)
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

## Configuration

Environment variables required:

-   `REPLICATE_API_TOKEN`: Your Replicate API token

Performance settings (`config/perf.js`):

-   `PERF.image.maxSidePx`: Maximum image dimension (default: 1024)

## Error Handling

The service includes:

-   Input validation errors (400)
-   Replicate API errors with retry logic
-   Upload failures
-   Timeout protection
-   Request ID tracking for debugging

## Dependencies

-   `sharp`: Image processing and resizing
-   `replicate`: Replicate API client
-   R2 storage integration
-   Rate limiting and retry utilities

## Commercial Use

When using FLUX.1 Kontext Pro on Replicate, outputs can be used commercially in apps, marketing, or any business use without restrictions.

## Future Enhancements

Potential improvements:

-   Additional style presets
-   Custom style creation
-   Batch processing
-   Progress callbacks for long-running jobs
-   Advanced parameters (aspect ratio control, strength adjustment)
-   Style mixing capabilities

## Related Models

Black Forest Labs offers other Kontext variants:

-   **flux-kontext-dev**: Open-weight version (currently used)
-   **flux-kontext-pro**: State-of-the-art performance (THIS MODEL)
-   **flux-kontext-max**: Premium with improved typography

## Support

For issues or questions:

1. Check Replicate model documentation: https://replicate.com/black-forest-labs/flux-kontext-pro
2. Review application logs with request ID
3. Verify API token and quotas
4. Check rate limiting settings

---

**Last Updated**: November 17, 2025  
**Model Version**: flux-kontext-pro  
**API Version**: v1
