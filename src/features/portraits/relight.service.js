import { replicate } from "../../integrations/replicate/client.js";
import { withRetry } from "../../utils/retry.js";
import { withReplicateLimiter } from "../../utils/limiters.js";
import {
    uploadBufferToR2,
    getImageUrl,
    // presignGetUrl, // <- KHÔNG CẦN cho input nữa
} from "../../integrations/r2/storage.service.js";
import { prescaleImage } from "../../utils/image.js";
import { PERF } from "../../config/perf.js";

const MODEL =
    "zsxkib/ic-light:d41bcb10d8c159868f4cfbd7c6a2ca01484f7d39e4613419d5952c61562f1ba7";

// tải nhị phân nếu client đưa image_url
async function fetchBinary(url, timeoutMs = PERF.http.timeoutMs) {
    const ctrl = new AbortController();
    const to = setTimeout(() => ctrl.abort(), timeoutMs);
    try {
        const resp = await fetch(url, { signal: ctrl.signal });
        if (!resp.ok) throw new Error(`Fetch ${url} failed: ${resp.status}`);
        return Buffer.from(await resp.arrayBuffer());
    } finally {
        clearTimeout(to);
    }
}

/**
 * IC-Light relight (phương án B: upload trực tiếp lên Replicate)
 * @param {Object} p
 * @param {Buffer|null} p.fileBuffer
 * @param {string|null} p.imageUrl
 * @param {string} p.prompt
 * @param {string} p.appended_prompt
 * @param {string} p.negative_prompt
 * @param {"None"|"Left Light"|"Right Light"|"Top Light"|"Bottom Light"} p.light_source
 * @param {number} p.steps
 * @param {number} p.cfg
 * @param {number} [p.width]
 * @param {number} [p.height]
 * @param {number} p.number_of_images
 * @param {"webp"|"jpg"|"png"} p.output_format
 * @param {number} p.output_quality
 * @param {string} p.requestId
 */
export async function icLightRelight(p) {
    const {
        fileBuffer,
        imageUrl,
        requestId,
        prompt,
        appended_prompt,
        negative_prompt,
        light_source,
        steps,
        cfg,
        width,
        height,
        number_of_images,
        output_format,
        output_quality,
    } = p;

    if (!fileBuffer && !imageUrl) {
        throw new Error("Missing image: provide file or image_url");
    }

    // ...
    const inputBuffer = fileBuffer
        ? fileBuffer
        : await fetchBinary(imageUrl, PERF.http.timeoutMs);
    const { buffer: preprocessed } = await prescaleImage(inputBuffer, {
        format: "jpeg",
    });

    const inputPayload = {
        subject_image: preprocessed, // Buffer directly
        prompt,
        appended_prompt,
        negative_prompt,
        steps,
        cfg,
        light_source,
        number_of_images,
        output_format,
        output_quality,
        ...(width ? { width } : {}),
        ...(height ? { height } : {}),
    };

    const prediction = await withRetry(
        () =>
            withReplicateLimiter(
                () =>
                    replicate.predictions.create({
                        version: MODEL,
                        input: inputPayload,
                    }),
                "heavy"
            ), // Heavy model - slow IC-Light
        {
            retries: PERF.retry.retries,
            factor: PERF.retry.factor,
            minTimeoutMs: PERF.retry.minTimeoutMs,
            maxTimeoutMs: PERF.retry.maxTimeoutMs,
        }
    );
    // poll → fetch outputs → upload to R2 (giữ nguyên)

    // 6) Poll kết quả
    let final;
    const started = Date.now();
    while (true) {
        final = await replicate.predictions.get(prediction.id);
        if (["succeeded", "failed", "canceled"].includes(final.status)) break;
        if (Date.now() - started > PERF.ai.maxJobMs) {
            throw new Error("Processing timeout");
        }
        await new Promise((r) => setTimeout(r, PERF.ai.pollIntervalMs));
    }
    if (final.status !== "succeeded") {
        throw new Error(`Replicate failed: ${final.status}`);
    }

    // 7) Xử lý output (mảng URL) -> tải về -> upload R2 -> presign kết quả
    // PARALLEL UPLOAD: Upload all outputs simultaneously instead of sequentially
    const outList = Array.isArray(final.output) ? final.output : [final.output];
    if (!outList?.length) throw new Error("Empty output from model");

    const uploadPromises = outList.map(async (url, i) => {
        const resultResp = await fetch(url);
        if (!resultResp.ok)
            throw new Error(`Fetch output failed: ${resultResp.status}`);
        const resultBuffer = Buffer.from(await resultResp.arrayBuffer());

        const ext =
            output_format === "png"
                ? "png"
                : output_format === "jpg"
                ? "jpg"
                : "webp";
        const outKey = `portraits/iclight/${Date.now()}-${Math.random()
            .toString(36)
            .slice(2)}-after-${i}.${ext}`;

        const uploaded = await uploadBufferToR2(resultBuffer, {
            key: outKey,
            contentType:
                output_format === "png"
                    ? "image/png"
                    : output_format === "jpg"
                    ? "image/jpeg"
                    : "image/webp",
            cacheControl: "public, max-age=31536000, immutable",
        });

        // Trả public URL cho FE
        return await getImageUrl(uploaded.key, 3600);
    });

    const afterUrls = await Promise.all(uploadPromises);

    return {
        // Không còn before_url từ R2 trong phương án B; nếu cần vẫn có thể trả lại link tạm của Replicate để debug
        output_url: afterUrls[0],
        output_urls: afterUrls,
        meta: {
            model: MODEL,
            prompt,
            light_source,
            steps,
            cfg,
            width,
            height,
            number_of_images,
            replicate_prediction_id: final.id,
            strategy: "upload-to-replicate", // đánh dấu đang dùng phương án B
        },
    };
}
