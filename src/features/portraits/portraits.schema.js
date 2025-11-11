import { z } from "zod";

// Client có thể gửi file (multer) HOẶC image_url (public/presigned)
const LightSourceEnum = z.enum([
    "None",
    "Left Light",
    "Right Light",
    "Top Light",
    "Bottom Light",
]);

export const IcLightSchema = z.object({
    image_url: z.string().url().optional(),

    // Text-guided relighting
    prompt: z
        .string()
        .min(1, "prompt is required")
        .default("studio soft light, flattering portrait lighting"),
    appended_prompt: z.string().default("best quality"),
    negative_prompt: z
        .string()
        .default("lowres, bad anatomy, bad hands, cropped, worst quality"),

    // Size (replicate enum list; ta validate mềm, server vẫn pass số)
    width: z.number().int().optional(), // 256..1024 theo step 64
    height: z.number().int().optional(),

    steps: z.number().int().min(1).max(100).default(25),
    cfg: z.number().min(1).max(32).default(2),
    light_source: LightSourceEnum.default("None"),
    number_of_images: z.number().int().min(1).max(12).default(1),

    output_format: z.enum(["webp", "jpg", "png"]).default("webp"),
    output_quality: z.number().int().max(100).default(80),
});

export function parseIcLightBody(body) {
    const parsed = IcLightSchema.safeParse(body);
    if (!parsed.success) {
        throw new Error(parsed.error.issues?.[0]?.message || "Invalid body");
    }
    return parsed.data;
}
