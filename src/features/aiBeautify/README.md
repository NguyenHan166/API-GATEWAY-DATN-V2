# AI Beautify Feature

## Overview

AI Beautify is an advanced image enhancement pipeline that combines multiple AI models and image processing techniques to create professional portrait enhancements.

## Pipeline Steps

### 1. Image Input

-   Receives image buffer from client
-   Supports: JPEG, PNG, WebP
-   Max file size: 10MB

### 2. Pre-scaling

-   Automatically scales images to max 1440px on longest side
-   Preserves aspect ratio
-   Reduces processing time and costs
-   **Important**: GFPGAN GPU limit ~2M pixels (1440x1440 = 2.07M safe)

### 3. GFPGAN Face Restoration

-   **Model**: `tencentarc/gfpgan` (v1.4)
-   **Purpose**: Restore facial details, fix imperfections
-   **Scale**: 2x
-   Uses Replicate API with retry logic
-   Handles upload failures gracefully

### 4. Real-ESRGAN Enhancement

-   **Model**: `nightmareai/real-esrgan`
-   **Purpose**: Overall image enhancement with face focus
-   **Parameters**:
    -   `scale`: 2 (then resized back to original dimensions)
    -   `face_enhance`: true
-   Further refines facial details and overall image quality

### 5. Skin Segmentation & Masking

-   Uses color-based skin tone detection
-   HSV color space analysis
-   Creates binary mask of skin regions
-   Fallback if MediaPipe is unavailable

### 6. Skin Retouching

-   Applies gentle blur (sigma: 1.4) to skin areas only
-   Preserves non-skin details (eyes, hair, clothing)
-   Creates smooth, natural-looking skin
-   Composites blurred skin back onto image

### 7. Tone Enhancement

-   **Brightness**: +3% (subtle lift)
-   **Saturation**: +5% (vibrant colors)
-   Final color grading for professional look

### 8. R2 Storage

-   Uploads final enhanced image to Cloudflare R2
-   Generates unique key with timestamp
-   Stores in `aiBeautify/` prefix

### 9. URL Generation

-   **Presigned URL**: Temporary access (1 hour)
-   **Public URL**: Permanent access (if configured)

## API Endpoint

### `POST /api/ai-beautify`

**Request:**

```http
POST /api/ai-beautify
Content-Type: multipart/form-data

image: [binary file]
```

**Response:**

```json
{
    "request_id": "abc123",
    "status": "success",
    "data": {
        "key": "aiBeautify/2025-11-18/uuid.jpg",
        "url": "https://your-public-url.com/aiBeautify/2025-11-18/uuid.jpg",
        "expires_in": 3600
    },
    "meta": {
        "bytes": 245678,
        "requestId": "abc123",
        "pipeline": [
            "pre-scale",
            "gfpgan",
            "real-esrgan",
            "skin-retouch",
            "tone-enhance"
        ]
    }
}
```

## Rate Limiting

-   **Limit**: 30 requests per minute per IP
-   Stricter than other endpoints due to resource intensity

## Error Handling

-   Validates file type and size
-   Retries on transient Replicate failures
-   Falls back gracefully if skin retouch fails
-   Detailed error messages with request ID

## Performance Considerations

### Processing Time

-   **GFPGAN**: ~15-30 seconds
-   **Real-ESRGAN**: ~10-20 seconds
-   **Skin Retouch**: ~1-2 seconds
-   **Total**: ~30-60 seconds per image

### Resource Usage

-   Uses `withReplicateLimiter` to prevent overwhelming Replicate API
-   Pre-scaling reduces API costs
-   Efficient buffer handling (no unnecessary file I/O)

## Technical Notes

### Skin Tone Detection

Current implementation uses RGB-based detection:

-   Rule 1: R > 95, G > 40, B > 20
-   Rule 2: Max(R,G,B) - Min(R,G,B) > 15
-   Rule 3: |R-G| > 15, R > G, R > B

For production, consider:

-   MediaPipe Face Mesh for accurate segmentation
-   TensorFlow.js with face-parser model
-   Deep learning-based skin segmentation

### Future Enhancements

1. **MediaPipe Integration**: More accurate skin/face detection
2. **Makeup Application**: Add optional makeup styles
3. **Teeth Whitening**: Detect and enhance smile
4. **Eye Enhancement**: Brighten and sharpen eyes
5. **Blemish Removal**: Automatic spot removal
6. **Background Blur**: Portrait mode effect
7. **Lighting Adjustment**: HDR-style enhancements

## Dependencies

-   `sharp`: Image processing
-   `replicate`: AI model inference
-   `@aws-sdk/client-s3`: R2 storage
-   `p-limit`: Concurrency control

## Examples

### Before & After

The pipeline transforms:

-   Blurry/low-quality portraits → Sharp, detailed faces
-   Uneven skin tone → Smooth, natural skin
-   Flat colors → Vibrant, professional look
-   Low contrast → Enhanced depth and dimension

### Use Cases

-   Profile picture enhancement
-   Social media photo editing
-   E-commerce product photos (models)
-   Dating app photos
-   Professional headshots
-   Event photography touch-ups

## Monitoring

The service includes:

-   Request ID tracking
-   Pipeline step logging
-   Error reporting with context
-   Performance metrics (buffer sizes)

## Cost Optimization

1. Pre-scaling reduces Replicate API costs
2. Sequential processing prevents rate limits
3. Retry logic handles transient failures
4. Efficient buffer reuse (no temp files)
