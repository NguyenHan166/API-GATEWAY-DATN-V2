import { S3Client } from "@aws-sdk/client-s3";
import { env } from "../../config/env.js";

export const r2 = new S3Client({
    region: "auto",
    endpoint: env.CF_R2_ENDPOINT,
    credentials: {
        accessKeyId: env.CF_R2_ACCESS_KEY_ID,
        secretAccessKey: env.CF_R2_SECRET_ACCESS_KEY,
    },
    forcePathStyle: true,
});
