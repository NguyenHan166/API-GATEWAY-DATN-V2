/**
 * Utility để chuẩn hóa response format cho tất cả API endpoints
 */

/**
 * Response chuẩn cho các API xử lý ảnh đơn
 * @param {Object} params
 * @param {string} params.requestId - Request ID từ middleware
 * @param {string} params.key - R2 storage key
 * @param {string} [params.url] - Public URL hoặc presigned URL
 * @param {string} [params.presignedUrl] - Presigned URL (nếu khác url)
 * @param {number} [params.expiresIn] - Thời gian hết hạn (giây)
 * @param {Object} [params.meta] - Metadata bổ sung
 * @param {string} [params.status] - Status (mặc định: "success")
 */
export function successResponse({
    requestId,
    key,
    url,
    presignedUrl,
    expiresIn = 3600,
    meta = {},
    status = "success",
}) {
    return {
        request_id: requestId,
        status,
        data: {
            key,
            url: url || presignedUrl,
            ...(presignedUrl && url !== presignedUrl
                ? { presigned_url: presignedUrl }
                : {}),
            ...(expiresIn ? { expires_in: expiresIn } : {}),
        },
        meta,
    };
}

/**
 * Response chuẩn cho các API xử lý nhiều ảnh (batch)
 * @param {Object} params
 * @param {string} params.requestId - Request ID từ middleware
 * @param {Array} params.outputs - Mảng các output {key, url, ...}
 * @param {Object} [params.meta] - Metadata bổ sung
 * @param {string} [params.status] - Status (mặc định: "success")
 */
export function successResponseMultiple({
    requestId,
    outputs,
    meta = {},
    status = "success",
}) {
    return {
        request_id: requestId,
        status,
        data: {
            outputs,
        },
        meta,
    };
}

/**
 * Response lỗi chuẩn
 * @param {Object} params
 * @param {string} [params.requestId] - Request ID từ middleware
 * @param {string} params.error - Thông báo lỗi
 * @param {string} [params.code] - Mã lỗi
 * @param {Object} [params.details] - Chi tiết lỗi
 * @param {string} [params.status] - Status (mặc định: "error")
 */
export function errorResponse({
    requestId,
    error,
    code = "BAD_REQUEST",
    details = null,
    status = "error",
}) {
    const response = {
        status,
        error: {
            message: error,
            code,
        },
    };

    if (requestId) {
        response.request_id = requestId;
    }

    if (details) {
        response.error.details = details;
    }

    return response;
}

/**
 * Response cho paginated data (manifest, listings, ...)
 * @param {Object} params
 * @param {string} [params.requestId] - Request ID
 * @param {Array} params.items - Dữ liệu items
 * @param {number} params.total - Tổng số items
 * @param {number} params.page - Trang hiện tại
 * @param {number} params.pageSize - Số items mỗi trang
 * @param {number} params.totalPages - Tổng số trang
 * @param {Object} [params.meta] - Metadata bổ sung
 * @param {string} [params.status] - Status (mặc định: "success")
 */
export function paginatedResponse({
    requestId,
    items,
    total,
    page,
    pageSize,
    totalPages,
    meta = {},
    status = "success",
}) {
    const response = {
        status,
        data: {
            items,
        },
        pagination: {
            total,
            page,
            page_size: pageSize,
            total_pages: totalPages,
        },
    };

    if (requestId) {
        response.request_id = requestId;
    }

    if (Object.keys(meta).length > 0) {
        response.meta = meta;
    }

    return response;
}
