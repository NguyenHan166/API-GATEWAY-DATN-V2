import { z } from "zod";

export const manifestQuerySchema = z.object({
    category: z.string().optional(),
    target: z.string().optional(),
    page: z.coerce.number().int().min(1).default(1),
    page_size: z.coerce.number().int().min(1).max(500).default(50),
});

export const presignReqSchema = z.object({
    pack_id: z.string(),
    key: z.string(),
});
