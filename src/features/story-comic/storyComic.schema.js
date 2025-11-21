import { z } from "zod";

const GenerateRequestSchema = z.object({
    prompt: z.string().min(8, "prompt too short"),
    pages: z.coerce
        .number()
        .int()
        .min(2, "pages must be 2 or 3")
        .max(3, "pages must be 2 or 3")
        .default(3),
    panels_per_page: z.coerce
        .number()
        .int()
        .min(3, "panels per page must be 3 or 4")
        .max(4, "panels per page must be 3 or 4")
        .default(4),
});

export function parseStoryComicBody(body) {
    const parsed = GenerateRequestSchema.safeParse(body);
    if (!parsed.success) {
        return { ok: false, error: parsed.error.format() };
    }
    return { ok: true, value: parsed.data };
}
