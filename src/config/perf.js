// Cấu hình hiệu năng có thể tái sử dụng cho mọi feature
export const PERF = {
    // Threadpool cho tác vụ CPU (sharp, crypto, zlib...). Set qua env khi start process.
    // Ví dụ: UV_THREADPOOL_SIZE=8 node src/server.js
    UV_THREADPOOL_SIZE: Number(process.env.UV_THREADPOOL_SIZE || 0), // 0 = mặc định Node

    // Giới hạn đồng thời cho các tác vụ nặng (p-limit)
    concurrency: {
        replaceBg: Number(process.env.CONCURRENCY_REPLACE_BG || 6),
        defaultHeavy: Number(process.env.CONCURRENCY_DEFAULT_HEAVY || 6),
    },

    // Timeout (ms)
    timeouts: {
        requestMs: Number(process.env.REQUEST_TIMEOUT_MS || 120_000), // 120s cho mỗi request
        replicateMs: Number(process.env.REPLICATE_TIMEOUT_MS || 180_000), // 180s cho model call
        r2Ms: Number(process.env.R2_TIMEOUT_MS || 30_000), // 30s cho R2
        httpMs: Number(process.env.HTTP_TIMEOUT_MS || 30_000), // 30s cho các HTTP request thông thường
    },

    // Upload/Body size
    body: {
        jsonLimit: process.env.JSON_LIMIT || "10mb",
        // Multer memory limits: set ở middleware upload nếu cần
    },

    // Resize trước khi xử lý (tiết kiệm CPU/RAM)
    image: {
        maxSidePx: Number(process.env.IMAGE_MAX_SIDE_PX || 2048), // scale long edge ≤ 2048
    },
};
