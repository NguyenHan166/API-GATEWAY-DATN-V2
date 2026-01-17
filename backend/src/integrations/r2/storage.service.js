// integrations/r2/storage.service.js
import {
    PutObjectCommand,
    GetObjectCommand,
    ListObjectsV2Command,
    HeadObjectCommand,
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { r2 } from "./client.js";
import { env } from "../../config/env.js";
import { randomUUID } from "crypto";
import { withRetry } from "../../utils/retry.js";

/** Convert Readable stream -> Buffer (dành cho nơi nào còn lỡ truyền stream) */
export async function readableToBuffer(readable) {
    const chunks = [];
    for await (const c of readable)
        chunks.push(Buffer.isBuffer(c) ? c : Buffer.from(c));
    return Buffer.concat(chunks);
}

/**
 * Upload buffer lên R2 — KHÔNG dùng stream.
 * - Giữ nguyên signature cũ (buffer, options)
 * - Thêm tùy chọn 'key' nếu muốn set đường dẫn cố định (không bắt buộc)
 */
export async function uploadBufferToR2(
    buffer,
    { contentType = "image/png", ext = "png", prefix = "outputs", key } = {}
) {
    if (!Buffer.isBuffer(buffer)) {
        throw new Error(
            "uploadBufferToR2 expects a Buffer. Use readableToBuffer(stream) first if needed."
        );
    }

    const finalKey =
        key ||
        `${prefix}/${new Date()
            .toISOString()
            .slice(0, 10)}/${randomUUID()}.${ext}`;

    await withRetry(
        () =>
            r2.send(
                new PutObjectCommand({
                    Bucket: env.CF_R2_BUCKET,
                    Key: finalKey,
                    Body: buffer, // Buffer, không phải stream
                    ContentType: contentType,
                    ContentLength: buffer.length, // quan trọng để tránh lỗi checksum
                })
            ),
        { retries: 2, baseDelayMs: 500 }
    );

    return { key: finalKey };
}

/**
 * Helper không phá API cũ: nếu nơi nào đó vẫn vô tình truyền stream,
 * hãy gọi hàm này — nó sẽ tự chuyển sang Buffer rồi gọi uploadBufferToR2.
 * (Không bắt buộc dùng; chỉ thêm để thuận tiện.)
 */
export async function uploadMaybeStreamToR2(maybeStreamOrBuffer, options) {
    const buf = Buffer.isBuffer(maybeStreamOrBuffer)
        ? maybeStreamOrBuffer
        : await readableToBuffer(maybeStreamOrBuffer);
    return uploadBufferToR2(buf, options);
}

/**
 * Get URL cho image - ưu tiên public URL, fallback presigned nếu không có
 * Đây là function chính nên dùng để trả URL về FE
 */
export async function getImageUrl(key, expiresIn = 3600) {
    // Ưu tiên public URL nếu có cấu hình
    const publicUrl = buildPublicUrl(key);
    if (publicUrl) {
        return publicUrl;
    }

    // Fallback: presigned URL cho private bucket
    return await presignGetUrl(key, expiresIn);
}

/**
 * Tạo presigned URL (chỉ dùng khi cần temporary access cho private bucket)
 * Thông thường nên dùng getImageUrl() thay vì function này
 */
export async function presignGetUrl(key, expiresIn = 3600) {
    // getSignedUrl đã internal call 1 lần; ta vẫn bọc retry cho chắc
    return await withRetry(
        () =>
            getSignedUrl(
                r2,
                new GetObjectCommand({ Bucket: env.CF_R2_BUCKET, Key: key }),
                { expiresIn }
            ),
        { retries: 2, baseDelayMs: 300 }
    );
}

/**
 * Trả public URL nếu có cấu hình base URL public.
 * Mặc định TRẢ VỀ NULL để tránh vô tình lộ endpoint nội bộ.
 * Nếu muốn giữ fallback cũ, set env R2_PUBLIC_FALLBACK=1.
 */
export function buildPublicUrl(key) {
    if (env.R2_PUBLIC_BASE_URL) {
        return `${env.R2_PUBLIC_BASE_URL.replace(/\/+$/, "")}/${key}`;
    }
    if (String(env.R2_PUBLIC_FALLBACK || "").trim() === "1") {
        return `${env.CF_R2_ENDPOINT.replace(/\/+$/, "")}/${
            env.CF_R2_BUCKET
        }/${key}`;
    }
    return null;
}

export async function listObjects(prefix = "", continuationToken) {
    return await withRetry(
        () =>
            r2.send(
                new ListObjectsV2Command({
                    Bucket: env.CF_R2_BUCKET,
                    Prefix: prefix || undefined,
                    ContinuationToken: continuationToken || undefined,
                    MaxKeys: 1000,
                })
            ),
        { retries: 2, baseDelayMs: 400 }
    );
}

export async function headObject(key) {
    try {
        return await withRetry(
            () =>
                r2.send(
                    new HeadObjectCommand({
                        Bucket: env.CF_R2_BUCKET,
                        Key: key,
                    })
                ),
            { retries: 1, baseDelayMs: 300 }
        );
    } catch (e) {
        if (e?.$metadata?.httpStatusCode === 404) return null;
        throw e;
    }
}
