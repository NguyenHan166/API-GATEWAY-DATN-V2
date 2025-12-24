import { z } from "zod";
import {
    DEFAULT_QUALITY_SELECTOR,
    DEFAULT_STYLE_SELECTOR,
    QUALITY_SELECTOR_OPTIONS,
    STYLE_SELECTOR_OPTIONS,
} from "./storyComic.constants.js";

/**
 * Create a case-insensitive enum schema that maps input to canonical value
 * @param {string[]} options - Array of canonical option values
 * @param {string} defaultValue - Default value if not provided
 */
function caseInsensitiveEnum(options, defaultValue) {
    const lowerToCanonical = new Map(
        options.map((opt) => [opt.toLowerCase(), opt])
    );

    return z
        .string()
        .transform((val) => val.toLowerCase())
        .refine((val) => lowerToCanonical.has(val), {
            message: `Invalid value. Expected one of: ${options.join(", ")}`,
        })
        .transform((val) => lowerToCanonical.get(val))
        .default(defaultValue);
}

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
    style_selector: caseInsensitiveEnum(
        STYLE_SELECTOR_OPTIONS,
        DEFAULT_STYLE_SELECTOR
    ),
    quality_selector: caseInsensitiveEnum(
        QUALITY_SELECTOR_OPTIONS,
        DEFAULT_QUALITY_SELECTOR
    ),
});

export function parseStoryComicBody(body) {
    const parsed = GenerateRequestSchema.safeParse(body);
    if (!parsed.success) {
        return { ok: false, error: parsed.error.format() };
    }
    return { ok: true, value: parsed.data };
}
