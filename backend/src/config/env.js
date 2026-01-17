import "dotenv/config";
import { z } from "zod";

const EnvSchema = z.object({
    NODE_ENV: z
        .enum(["development", "test", "production"])
        .default("development"),
    PORT: z.string().default("3000"),

    REPLICATE_API_TOKEN: z.string(),

    CF_R2_ACCOUNT_ID: z.string(),
    CF_R2_BUCKET: z.string(),
    CF_R2_ACCESS_KEY_ID: z.string(),
    CF_R2_SECRET_ACCESS_KEY: z.string(),
    CF_R2_ENDPOINT: z.string().url(),

    R2_PUBLIC_BASE_URL: z.string().url().optional(),
});

export const env = EnvSchema.parse(process.env);
