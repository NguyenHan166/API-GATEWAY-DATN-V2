import { z } from "zod";

const GenerateRequestSchema = z.object({
    prompt: z.string().min(10, "prompt too short (minimum 10 characters)"),
    pages: z.coerce
        .number()
        .int()
        .min(1, "at least 1 page")
        .max(3, "maximum 3 pages")
        .default(1),
    panelsPerPage: z.coerce
        .number()
        .int()
        .min(3, "at least 3 panels per page")
        .max(9, "maximum 9 panels per page")
        .default(6),
    style: z.string().default("comic book style art"),
});

export function parseGenerateBody(body) {
    const parsed = GenerateRequestSchema.safeParse(body);
    if (!parsed.success) {
        return { ok: false, error: parsed.error.format() };
    }
    return { ok: true, value: parsed.data };
}
