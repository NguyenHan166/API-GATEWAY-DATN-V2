import { z } from "zod";

export const replaceQuerySchema = z.object({
    mode: z.enum(["remove", "replace"]).default("replace"),
    fit: z.enum(["cover", "contain", "fill", "inside", "outside"]).nullish(),
    position: z.string().nullish(),
    featherPx: z.coerce.number().min(0).max(20).nullish(),
    shadow: z.enum(["0", "1"]).nullish(),
    signTtl: z.coerce
        .number()
        .min(60)
        .max(60 * 60 * 24)
        .nullish(),
});
