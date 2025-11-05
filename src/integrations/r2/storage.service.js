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

export async function uploadBufferToR2(
    buffer,
    { contentType = "image/png", ext = "png", prefix = "outputs" } = {}
) {
    const key = `${prefix}/${new Date()
        .toISOString()
        .slice(0, 10)}/${randomUUID()}.${ext}`;
    await withRetry(
        () =>
            r2.send(
                new PutObjectCommand({
                    Bucket: env.CF_R2_BUCKET,
                    Key: key,
                    Body: buffer,
                    ContentType: contentType,
                })
            ),
        { retries: 2, baseDelayMs: 500 }
    );
    return { key };
}

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

export function buildPublicUrl(key) {
    if (env.R2_PUBLIC_BASE_URL) return `${env.R2_PUBLIC_BASE_URL}/${key}`;
    return `${env.CF_R2_ENDPOINT}/${env.CF_R2_BUCKET}/${key}`;
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
