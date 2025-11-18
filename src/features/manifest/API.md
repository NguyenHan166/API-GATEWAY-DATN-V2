# Manifest API Documentation

## Overview

Service quản lý manifest của các resource packs (style packs, background packs, etc.). Cung cấp khả năng list, filter, phân trang các packs và tạo presigned URL để download files.

## Endpoints

### 1. GET `/api/manifest`

Lấy danh sách resource packs với khả năng filter và phân trang.

#### Request

##### Headers

```
Content-Type: application/json
```

##### Query Parameters

| Parameter   | Type   | Required | Description                                      | Default |
| ----------- | ------ | -------- | ------------------------------------------------ | ------- |
| `category`  | String | ❌       | Filter theo category (VD: "style", "background") | -       |
| `target`    | String | ❌       | Filter theo target (VD: "replace-style")         | -       |
| `page`      | Number | ❌       | Số trang (≥ 1)                                   | `1`     |
| `page_size` | Number | ❌       | Số items mỗi trang (1-500)                       | `50`    |

#### Response

##### Success (200 OK)

```json
{
    "request_id": "req_abc123",
    "status": "success",
    "data": {
        "items": [
            {
                "id": "styles/anime",
                "title": "styles — anime",
                "category": "styles",
                "target": "anime",
                "count": 15,
                "files": [
                    {
                        "key": "styles/anime/01.jpg",
                        "size": 245678,
                        "etag": "abc123def456",
                        "content_type": "application/octet-stream"
                    }
                ]
            }
        ]
    },
    "pagination": {
        "total": 42,
        "page": 1,
        "page_size": 50,
        "total_pages": 1
    },
    "meta": {
        "version": "2025.10.0"
    }
}
```

##### Error Response (400 Bad Request)

```json
{
    "request_id": "req_abc123",
    "status": "error",
    "error": {
        "message": "Invalid query parameters",
        "code": "VALIDATION_ERROR",
        "details": {
            "page": "Expected number, received string"
        }
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

### POST `/api/presign`

Tạo presigned URL để download file từ một pack cụ thể.

#### Request

##### Headers

```
Content-Type: application/json
```

##### Body Parameters

| Parameter | Type   | Required | Description          |
| --------- | ------ | -------- | -------------------- |
| `pack_id` | String | ✅       | ID của pack          |
| `key`     | String | ✅       | Key của file cần get |

##### Example Request Body

```json
{
    "pack_id": "styles/anime",
    "key": "styles/anime/01.jpg"
}
```

#### Response

**Response Success (200 OK)**:

```json
{
    "request_id": "e2aYWej9VHWu60oNS9DKE",
    "status": "success",
    "data": {
        "url": "https://d658d7ec8dd0cbdd02ce985566c8a042.r2.cloudflarestorage.com/filters-prod/ON1_BW_LUTs/For_Other_Programs/BW1.cube?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Credential=...",
        "expires_in": 3600
    }
}
```

##### Error Response (404 Not Found - Pack)

```json
{
    "request_id": "req_abc123",
    "status": "error",
    "error": {
        "message": "Pack not found",
        "code": "NOT_FOUND"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

##### Error Response (404 Not Found - File)

```json
{
    "request_id": "req_abc123",
    "status": "error",
    "error": {
        "message": "File not found in pack",
        "code": "NOT_FOUND"
    },
    "timestamp": "2025-11-18T10:30:00.000Z"
}
```

## Response Fields

### GET /api/manifest

| Field                       | Type   | Description                       |
| --------------------------- | ------ | --------------------------------- |
| `request_id`                | String | Unique request identifier         |
| `status`                    | String | "success" or "error"              |
| `data.items`                | Array  | List of resource packs            |
| `data.items[].id`           | String | Pack ID (format: category/target) |
| `data.items[].title`        | String | Display title of the pack         |
| `data.items[].category`     | String | Pack category                     |
| `data.items[].target`       | String | Target feature for this pack      |
| `data.items[].count`        | Number | Number of files in the pack       |
| `data.items[].files`        | Array  | List of files in the pack         |
| `data.items[].files[].key`  | String | R2 storage key                    |
| `data.items[].files[].size` | Number | File size in bytes                |
| `data.items[].files[].etag` | String | File ETag                         |
| `pagination.total`          | Number | Total number of packs             |
| `pagination.page`           | Number | Current page number               |
| `pagination.page_size`      | Number | Items per page                    |
| `pagination.total_pages`    | Number | Total number of pages             |
| `meta.version`              | String | Manifest version                  |

### POST /api/presign

| Field             | Type   | Description                                 |
| ----------------- | ------ | ------------------------------------------- |
| `request_id`      | String | Unique request identifier                   |
| `status`          | String | "success" or "error"                        |
| `data.url`        | String | Presigned URL for downloading the file      |
| `data.expires_in` | Number | URL expiration time in seconds (3600 = 1hr) |

## Examples

### GET Manifest - Filter by Category

#### cURL

```bash
curl -X GET "http://localhost:3000/api/manifest?category=style&page=1&page_size=10"
```

#### JavaScript (Fetch)

```javascript
const response = await fetch(
    "http://localhost:3000/api/manifest?category=style&page=1&page_size=10"
);
const result = await response.json();
console.log("Packs:", result.data.items);
```

#### Python

```python
import requests

response = requests.get(
    "http://localhost:3000/api/manifest",
    params={"category": "style", "page": 1, "page_size": 10}
)

result = response.json()
print(f"Found {result['data']['pagination']['total']} packs")
```

### POST Presign URL

#### cURL

```bash
curl -X POST http://localhost:3000/api/presign \
  -H "Content-Type: application/json" \
  -d '{
    "pack_id": "styles/anime",
    "key": "styles/anime/01.jpg"
  }'
```

#### JavaScript (Fetch)

```javascript
const response = await fetch("http://localhost:3000/api/presign", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
    },
    body: JSON.stringify({
        pack_id: "styles/anime",
        key: "styles/anime/01.jpg",
    }),
});

const result = await response.json();
console.log("Download URL:", result.data.url);
```

#### Python

```python
import requests

response = requests.post(
    'http://localhost:3000/api/presign',
    json={
        "pack_id": "styles/anime",
        "key": "styles/anime/01.jpg"
    }
)

result = response.json()
print(f"Download URL: {result['data']['url']}")
```

## Features

### Caching

-   Manifest data được cache trong memory
-   Auto-refresh khi có thay đổi
-   Giảm load lên storage backend

### Pagination

-   Default page size: 50 items
-   Max page size: 500 items
-   Efficient filtering và sorting

### Filtering

-   **Category**: Filter theo loại pack (style, background, etc.)
-   **Target**: Filter theo feature đích (replace-style, replace-bg, etc.)
-   Có thể combine nhiều filter

## Technical Details

### Data Source

Manifest được load từ:

-   Cloudflare R2 storage
-   File: `manifest.json`
-   Format: JSON với schema chuẩn

### URL Expiration

-   Presigned URLs expire sau 3600 giây (1 giờ)
-   Client nên request URL mới khi cần
-   URL không thể refresh, phải request lại

### Validation

-   Query params được validate bằng Zod schema
-   Page number phải ≥ 1
-   Page size trong khoảng 1-500
-   Pack ID và file key phải tồn tại trong manifest

## Error Codes

| Code               | HTTP Status | Description              | Solution                         |
| ------------------ | ----------- | ------------------------ | -------------------------------- |
| `VALIDATION_ERROR` | 400         | Invalid query parameters | Check parameter types and values |
| `NOT_FOUND`        | 404         | Pack or file not found   | Verify pack_id and key exist     |
| `INTERNAL_ERROR`   | 500         | Server error             | Retry or contact support         |

## Best Practices

### Frontend Integration

✅ **DO:**

-   Cache manifest data client-side
-   Request presigned URLs only when needed
-   Implement pagination for large lists
-   Handle 404 errors gracefully
-   Use category/target filters to reduce data

❌ **DON'T:**

-   Request entire manifest on every page load
-   Store presigned URLs for > 30 minutes
-   Request presigned URLs for all files at once
-   Ignore pagination for large datasets

### Performance

1. **Pagination**: Always use reasonable page_size (10-50 items)
2. **Filtering**: Apply filters server-side, not client-side
3. **Caching**: Cache manifest locally for 5-10 minutes
4. **Lazy Loading**: Request presigned URLs on-demand

## Use Cases

1. **Style Picker UI**

    - List style packs with previews
    - Download style files on selection
    - Show pack metadata

2. **Background Gallery**

    - Browse background packs
    - Filter by category
    - Download selected backgrounds

3. **Asset Management**
    - Admin panel for pack management
    - Bulk download capabilities
    - Pack version tracking

## Support

For issues or questions:

1. Check pack_id and key validity
2. Verify manifest.json is up to date
3. Ensure R2 credentials are configured
4. Review request_id for debugging
