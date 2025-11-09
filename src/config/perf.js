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

    // Upload/Body size
    body: {
        jsonLimit: process.env.JSON_LIMIT || "10mb",
        // Multer memory limits: set ở middleware upload nếu cần
    },

    // Resize trước khi xử lý (tiết kiệm CPU/RAM)
    image: {
        maxSidePx: Number(process.env.IMAGE_MAX_SIDE_PX || 2048), // scale long edge ≤ 2048
        outputQuality: Number(process.env.OUTPUT_QUALITY || 92),
    },

    retries: {
        attempts: Number(process.env.AI_RETRIES || 2),
        baseDelayMs: Number(process.env.AI_RETRY_BASE_MS || 800),
        factor: Number(process.env.AI_RETRY_FACTOR || 2),
    },

    r2: {
        presignExpiresSec: Number(process.env.R2_PRESIGN_EXPIRES || 3600),
    },
};
