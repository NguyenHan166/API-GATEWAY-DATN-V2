# Replace Background Feature

## Tổng quan

Feature này cung cấp 2 chức năng xử lý background cho ảnh:

1. **Remove Background** - Xóa nền ảnh, tạo ảnh PNG với nền trong suốt
2. **Replace Background** - Thay thế nền ảnh bằng một background mới

Sử dụng model AI từ Replicate: `851-labs/background-remover` để tách nền chính xác.

## API Endpoint

### POST `/api/replace-bg`

Endpoint duy nhất hỗ trợ cả 2 chế độ thông qua parameter `mode`.

## Request

### Headers

```
Content-Type: multipart/form-data
```

### Form Data

#### Chế độ Remove Background

| Field       | Type   | Required | Description                                                                  |
| ----------- | ------ | -------- | ---------------------------------------------------------------------------- |
| `fg`        | File   | ✅       | Ảnh cần xóa nền (foreground)                                                 |
| `mode`      | String | ❌       | Giá trị: `"remove"`. Nếu không truyền, mặc định là `"replace"`               |
| `featherPx` | Number | ❌       | Độ mượt của viền alpha (0-20). Mặc định: 1                                   |
| `signTtl`   | Number | ❌       | Thời gian sống của presigned URL (giây). Min: 60, Max: 86400. Mặc định: 3600 |

#### Chế độ Replace Background

| Field       | Type   | Required | Description                                                                                         |
| ----------- | ------ | -------- | --------------------------------------------------------------------------------------------------- |
| `fg`        | File   | ✅       | Ảnh foreground (ảnh muốn giữ)                                                                       |
| `bg`        | File   | ✅       | Ảnh background (nền mới)                                                                            |
| `mode`      | String | ❌       | Giá trị: `"replace"` (hoặc không truyền)                                                            |
| `fit`       | String | ❌       | Cách fit background: `"cover"`, `"contain"`, `"fill"`, `"inside"`, `"outside"`. Mặc định: `"cover"` |
| `position`  | String | ❌       | Vị trí background: `"centre"`, `"top"`, `"bottom"`, `"left"`, `"right"`, etc. Mặc định: `"centre"`  |
| `featherPx` | Number | ❌       | Độ mượt của viền alpha (0-20). Mặc định: 1                                                          |
| `shadow`    | String | ❌       | Thêm bóng đổ: `"0"` (không), `"1"` (có). Mặc định: `"1"`                                            |
| `signTtl`   | Number | ❌       | Thời gian sống của presigned URL (giây). Min: 60, Max: 86400. Mặc định: 3600                        |

## Response

### Success (200 OK)

```json
{
    "success": true,
    "requestId": "req_abc123",
    "data": {
        "key": "images/2025/11/17/uuid-abc123.png",
        "url": "https://pub-xxxx.r2.dev/images/2025/11/17/uuid-abc123.png",
        "presignedUrl": "https://pub-xxxx.r2.dev/images/2025/11/17/uuid-abc123.png?X-Amz-...",
        "expiresIn": 3600,
        "meta": {
            "width": 1024,
            "height": 768,
            "mode": "remove"
        }
    },
    "timestamp": "2025-11-17T10:30:00.000Z"
}
```

### Error Responses

#### 400 Bad Request - Missing Files

```json
{
    "success": false,
    "requestId": "req_abc123",
    "error": {
        "message": "Missing required file",
        "code": "MISSING_FILE",
        "details": "fg file is required"
    },
    "timestamp": "2025-11-17T10:30:00.000Z"
}
```

#### 400 Bad Request - Missing Background (Replace Mode)

```json
{
    "success": false,
    "requestId": "req_abc123",
    "error": {
        "message": "Missing required file",
        "code": "MISSING_FILE",
        "details": "bg file is required for replace mode"
    },
    "timestamp": "2025-11-17T10:30:00.000Z"
}
```

## Ví dụ sử dụng

### 1. Remove Background

#### cURL

```bash
curl -X POST http://localhost:3000/api/replace-bg \
  -F "fg=@portrait.jpg" \
  -F "mode=remove" \
  -F "featherPx=2"
```

#### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("fg", foregroundFile);
formData.append("mode", "remove");
formData.append("featherPx", "2");

const response = await fetch("http://localhost:3000/api/replace-bg", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log(result.data.url); // URL của ảnh đã xóa nền
```

#### Python

```python
import requests

files = {'fg': open('portrait.jpg', 'rb')}
data = {'mode': 'remove', 'featherPx': '2'}

response = requests.post(
    'http://localhost:3000/api/replace-bg',
    files=files,
    data=data
)

result = response.json()
print(result['data']['url'])
```

### 2. Replace Background

#### cURL

```bash
curl -X POST http://localhost:3000/api/replace-bg \
  -F "fg=@portrait.jpg" \
  -F "bg=@beach.jpg" \
  -F "mode=replace" \
  -F "fit=cover" \
  -F "position=centre" \
  -F "shadow=1" \
  -F "featherPx=2"
```

#### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append("fg", foregroundFile);
formData.append("bg", backgroundFile);
formData.append("mode", "replace"); // hoặc không cần vì mặc định là replace
formData.append("fit", "cover");
formData.append("position", "centre");
formData.append("shadow", "1");
formData.append("featherPx", "2");

const response = await fetch("http://localhost:3000/api/replace-bg", {
    method: "POST",
    body: formData,
});

const result = await response.json();
console.log(result.data.url); // URL của ảnh đã thay thế nền
```

#### Python

```python
import requests

files = {
    'fg': open('portrait.jpg', 'rb'),
    'bg': open('beach.jpg', 'rb')
}
data = {
    'mode': 'replace',
    'fit': 'cover',
    'position': 'centre',
    'shadow': '1',
    'featherPx': '2'
}

response = requests.post(
    'http://localhost:3000/api/replace-bg',
    files=files,
    data=data
)

result = response.json()
print(result['data']['url'])
```

## Chi tiết kỹ thuật

### Background Removal Model

-   **Model**: `851-labs/background-remover` (version pinned)
-   **Output Format**: PNG với alpha channel (RGBA)
-   **Retry Logic**: Tự động retry 2 lần nếu gặp lỗi

### Image Processing Pipeline

#### Remove Mode

1. Pre-scale ảnh nếu cần (giữ max side ≤ config `PERF.image.maxSidePx`)
2. Gọi model AI để remove background
3. Feather alpha channel để làm mượt viền
4. Upload lên R2 storage

#### Replace Mode

1. Pre-scale ảnh foreground nếu cần
2. Gọi model AI để remove background
3. Resize background để fit với foreground
4. Feather alpha channel của cutout
5. (Optional) Tạo bóng đổ từ alpha channel
6. Composite: background + shadow + foreground
7. Upload lên R2 storage

### Performance Considerations

-   **Pre-scaling**: Ảnh quá lớn sẽ được resize trước khi xử lý để tối ưu tốc độ
-   **Concurrency Limiting**: Sử dụng `p-limit` để giới hạn số request đồng thời
-   **Retry Strategy**: Exponential backoff với base delay 800ms, factor 2

### Fit Options

-   **cover**: Background phủ kín toàn bộ khung, crop phần thừa
-   **contain**: Background fit vừa khung, có thể có padding
-   **fill**: Stretch background để fill khung
-   **inside**: Resize background vừa khung, giữ aspect ratio
-   **outside**: Resize background phủ khung, giữ aspect ratio

### Position Options

Các giá trị hỗ trợ: `centre`, `center`, `top`, `right top`, `right`, `right bottom`, `bottom`, `left bottom`, `left`, `left top`

## Rate Limiting

-   **Window**: 60 giây
-   **Max Requests**: 60 requests/window
-   Requests vượt quá sẽ nhận HTTP 429 (Too Many Requests)

## Dependencies

### External Services

-   **Replicate API**: Để chạy model AI background removal
-   **Cloudflare R2**: Để lưu trữ ảnh output

### npm Packages

-   `sharp`: Image processing
-   `zod`: Schema validation
-   `p-limit`: Concurrency control

## Error Handling

Feature này sử dụng retry logic với exponential backoff để xử lý các lỗi tạm thời từ Replicate API. Nếu sau 2 lần retry vẫn lỗi, sẽ throw error về client.

## Best Practices

1. **File Size**: Nên resize ảnh về kích thước hợp lý trước khi upload (khuyến nghị max 2048px)
2. **File Format**: Hỗ trợ JPEG, PNG, WebP cho input
3. **Shadow**: Bật shadow (`shadow=1`) cho ảnh portrait để tự nhiên hơn
4. **Feather**: Sử dụng `featherPx=2-3` để viền mượt hơn
5. **Fit**: Dùng `fit=cover` cho background ảnh thực, `fit=contain` cho background màu trơn

## Troubleshooting

### Ảnh bị vỡ/nhiễu

-   Giảm `featherPx` xuống 0 hoặc 1
-   Kiểm tra chất lượng ảnh input

### Background không vừa

-   Thử các `fit` option khác nhau
-   Điều chỉnh `position` để đặt foreground ở vị trí phù hợp

### Request timeout

-   Resize ảnh nhỏ hơn trước khi upload
-   Thử lại request (có thể do Replicate API chậm tạm thời)

### 429 Too Many Requests

-   Giảm tần suất request
-   Implement client-side rate limiting hoặc queue system
