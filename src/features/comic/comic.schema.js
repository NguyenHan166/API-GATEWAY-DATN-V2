import { z } from "zod";

const GenerateRequestSchema = z.object({
    prompt: z.string().min(5, "prompt too short"),
    panels: z.coerce
        .number()
        .int()
        .min(1, "at least 1 panel")
        .max(6, "too many panels")
        .default(4),
    style: z.string().default("anime_color"),
});

export function parseGenerateBody(body) {
    const parsed = GenerateRequestSchema.safeParse(body);
    if (!parsed.success) {
        return { ok: false, error: parsed.error.format() };
    }
    return { ok: true, value: parsed.data };
}
