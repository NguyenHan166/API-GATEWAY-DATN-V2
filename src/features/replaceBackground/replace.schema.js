import { z } from "zod";

export const replaceQuerySchema = z.object({
    fit: z.enum(["cover", "contain", "fill", "inside", "outside"]).optional(),
    position: z.string().optional(),
    featherPx: z.coerce.number().min(0).max(20).optional(),
    shadow: z.enum(["0", "1"]).optional(),
    signTtl: z.coerce
        .number()
        .min(60)
        .max(60 * 60 * 24)
        .optional(),
});
