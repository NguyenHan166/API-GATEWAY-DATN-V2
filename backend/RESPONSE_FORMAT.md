# API Response Format Chuẩn

## Tổng quan

Tất cả các API endpoints trong hệ thống đều sử dụng format response chuẩn để đảm bảo tính nhất quán và dễ dàng tích hợp từ phía client.

## 1. Success Response - Đơn lẻ (Single Output)

Dành cho các API xử lý và trả về 1 kết quả (ảnh, file, ...)

### Format:

```json
{
    "request_id": "uuid-string",
    "status": "success",
    "data": {
        "key": "r2-storage-key",
        "url": "public-or-presigned-url",
        "presigned_url": "presigned-url-if-different",
        "expires_in": 3600
    },
    "meta": {
        // Metadata tùy theo từng API
    }
}
```

### Ví dụ thực tế:

**GFPGAN Enhancement:**

```json
{
    "request_id": "abc-123-def",
    "status": "success",
    "data": {
        "key": "gfpgan/s2-v1.4/1234567890.jpg",
        "url": "https://pub.example.com/gfpgan/s2-v1.4/1234567890.jpg",
        "presigned_url": "https://r2.example.com/gfpgan/...?signature=...",
        "expires_in": 3600
    },
    "meta": {
        "scale": 2,
        "version": "1.4",
        "bytes": 256000,
        "requestId": "abc-123-def"
    }
}
```

**Replace Style:**

```json
{
    "request_id": "xyz-789",
    "status": "success",
    "data": {
        "key": "styles/anime/1234567890.jpg",
        "url": "https://pub.example.com/styles/anime/1234567890.jpg",
        "expires_in": 3600
    },
    "meta": {
        "style": "anime",
        "bytes": 180000,
        "requestId": "xyz-789"
    }
}
```

**Replace Background:**

```json
{
    "request_id": "bg-456",
    "status": "success",
    "data": {
        "key": "replace-bg/1234567890.png",
        "url": "https://pub.example.com/replace-bg/1234567890.png",
        "presigned_url": "https://r2.example.com/replace-bg/...?signature=...",
        "expires_in": 3600
    },
    "meta": {
        "width": 1024,
        "height": 768
    }
}
```

## 2. Success Response - Nhiều kết quả (Multiple Outputs)

Dành cho các API xử lý và trả về nhiều kết quả (batch processing)

### Format:

```json
{
    "request_id": "uuid-string",
    "status": "success",
    "data": {
        "outputs": [
            {
                "url": "output-url-1",
                "index": 0
            },
            {
                "url": "output-url-2",
                "index": 1
            }
        ]
    },
    "meta": {
        // Metadata tùy theo từng API
    }
}
```

### Ví dụ thực tế:

**IC-Light Portraits:**

```json
{
    "request_id": "portrait-123",
    "status": "success",
    "data": {
        "outputs": [
            {
                "url": "https://r2.example.com/portraits/iclight/...?signature=...",
                "index": 0
            },
            {
                "url": "https://r2.example.com/portraits/iclight/...?signature=...",
                "index": 1
            }
        ]
    },
    "meta": {
        "model": "zsxkib/ic-light:d41bcb10d8c159868...",
        "prompt": "Beautiful portrait with natural lighting",
        "light_source": "Left Light",
        "steps": 25,
        "cfg": 7,
        "width": 1024,
        "height": 1024,
        "number_of_images": 2,
        "replicate_prediction_id": "pred-abc123",
        "strategy": "upload-to-replicate"
    }
}
```

## 3. Error Response

Tất cả các lỗi đều sử dụng format này

### Format:

```json
{
    "request_id": "uuid-string",
    "status": "error",
    "error": {
        "message": "Human-readable error message",
        "code": "ERROR_CODE",
        "details": "Additional details (optional)"
    }
}
```

### Ví dụ thực tế:

**Validation Error:**

```json
{
    "request_id": "req-789",
    "status": "error",
    "error": {
        "message": "Invalid input",
        "code": "VALIDATION_ERROR",
        "details": "scale must be between 1 and 4"
    }
}
```

**Missing Files:**

```json
{
    "request_id": "req-456",
    "status": "error",
    "error": {
        "message": "Missing required files",
        "code": "MISSING_FILES",
        "details": "Both fg and bg files are required"
    }
}
```

**Processing Error:**

```json
{
    "request_id": "req-123",
    "status": "error",
    "error": {
        "message": "Processing timeout",
        "code": "PROCESSING_ERROR",
        "details": null
    }
}
```

**Not Found:**

```json
{
    "request_id": "req-999",
    "status": "error",
    "error": {
        "message": "Pack not found",
        "code": "NOT_FOUND",
        "details": null
    }
}
```

## 4. Paginated Response

Dành cho các API trả về danh sách có phân trang

### Format:

```json
{
    "request_id": "uuid-string",
    "status": "success",
    "data": {
        "items": [
            // Array of items
        ]
    },
    "pagination": {
        "total": 100,
        "page": 1,
        "page_size": 20,
        "total_pages": 5
    },
    "meta": {
        // Metadata tùy theo từng API
    }
}
```

### Ví dụ thực tế:

**Manifest Listing:**

```json
{
    "request_id": "manifest-123",
    "status": "success",
    "data": {
        "items": [
            {
                "id": "pack-001",
                "title": "Summer Collection",
                "category": "backgrounds",
                "target": "portraits",
                "files": [
                    {
                        "key": "backgrounds/summer/beach-01.jpg",
                        "name": "beach-01.jpg",
                        "size": 256000
                    }
                ],
                "count": 10
            }
        ]
    },
    "pagination": {
        "total": 100,
        "page": 1,
        "page_size": 20,
        "total_pages": 5
    },
    "meta": {
        "version": "1.0.0"
    }
}
```

## 5. Mã lỗi chuẩn (Error Codes)

| Code                  | Mô tả                     | HTTP Status |
| --------------------- | ------------------------- | ----------- |
| `VALIDATION_ERROR`    | Lỗi validate input        | 400         |
| `MISSING_FILES`       | Thiếu file upload         | 400         |
| `PROCESSING_ERROR`    | Lỗi xử lý                 | 400/500     |
| `NOT_FOUND`           | Không tìm thấy resource   | 404         |
| `TIMEOUT_ERROR`       | Request timeout           | 408         |
| `RATE_LIMIT_EXCEEDED` | Vượt quá giới hạn request | 429         |
| `INTERNAL_ERROR`      | Lỗi server nội bộ         | 500         |

## 6. Best Practices cho Client

### Kiểm tra response:

```javascript
async function callAPI(endpoint, data) {
    const response = await fetch(endpoint, {
        method: "POST",
        body: data,
    });

    const result = await response.json();

    // Kiểm tra status
    if (result.status === "success") {
        // Xử lý success
        console.log("Request ID:", result.request_id);

        // Single output
        if (result.data.url) {
            console.log("Output URL:", result.data.url);
        }

        // Multiple outputs
        if (result.data.outputs) {
            result.data.outputs.forEach((output) => {
                console.log(`Output ${output.index}:`, output.url);
            });
        }

        // Paginated data
        if (result.data.items) {
            console.log("Items:", result.data.items);
            console.log(
                "Page:",
                result.pagination.page,
                "/",
                result.pagination.total_pages
            );
        }
    } else {
        // Xử lý error
        console.error("Error:", result.error.message);
        console.error("Code:", result.error.code);
        if (result.error.details) {
            console.error("Details:", result.error.details);
        }
    }
}
```

### TypeScript Types:

```typescript
// Base response
interface BaseResponse {
    request_id: string;
    status: "success" | "error";
}

// Success response - single
interface SingleSuccessResponse extends BaseResponse {
    status: "success";
    data: {
        key: string;
        url: string;
        presigned_url?: string;
        expires_in?: number;
    };
    meta?: Record<string, any>;
}

// Success response - multiple
interface MultipleSuccessResponse extends BaseResponse {
    status: "success";
    data: {
        outputs: Array<{
            url: string;
            index: number;
        }>;
    };
    meta?: Record<string, any>;
}

// Error response
interface ErrorResponse extends BaseResponse {
    status: "error";
    error: {
        message: string;
        code: string;
        details?: any;
    };
}

// Paginated response
interface PaginatedResponse<T> extends BaseResponse {
    status: "success";
    data: {
        items: T[];
    };
    pagination: {
        total: number;
        page: number;
        page_size: number;
        total_pages: number;
    };
    meta?: Record<string, any>;
}
```

## 7. Migration Guide

Nếu bạn đang sử dụng API phiên bản cũ, đây là cách migrate:

### Trước (Old Format):

```json
{
  "key": "...",
  "url": "...",
  "presigned_url": "...",
  "expires_in": 3600,
  "meta": {...}
}
```

### Sau (New Format):

```json
{
  "request_id": "...",
  "status": "success",
  "data": {
    "key": "...",
    "url": "...",
    "presigned_url": "...",
    "expires_in": 3600
  },
  "meta": {...}
}
```

### Code migration:

```javascript
// Old way
const { key, url, meta } = await response.json();

// New way
const { request_id, status, data, meta } = await response.json();
const { key, url } = data;

// Or với destructuring
const {
    data: { key, url },
    meta,
} = await response.json();
```
