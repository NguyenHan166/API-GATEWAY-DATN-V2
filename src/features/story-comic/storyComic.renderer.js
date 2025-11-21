import sharp from "sharp";
import { logger } from "../../config/logger.js";

const PAGE_WIDTH = 1080;
const PAGE_HEIGHT = 1620;
const GAP = 24;

async function loadCanvasLib() {
    try {
        return await import("@napi-rs/canvas");
    } catch (err) {
        logger.warn(
            { err: err?.message },
            "Falling back to sharp renderer (canvas not available)"
        );
        return null;
    }
}

function layoutForPanels(count) {
    if (count === 3) {
        const topHeight = Math.round(PAGE_HEIGHT * 0.55);
        const bottomHeight =
            (PAGE_HEIGHT - topHeight - GAP * 3) / 2 > 0
                ? Math.round((PAGE_HEIGHT - topHeight - GAP * 3) / 2)
                : Math.round((PAGE_HEIGHT - GAP * 4) / 3);
        return [
            {
                x: GAP,
                y: GAP,
                width: PAGE_WIDTH - GAP * 2,
                height: topHeight - GAP,
            },
            {
                x: GAP,
                y: topHeight + GAP,
                width: (PAGE_WIDTH - GAP * 3) / 2,
                height: bottomHeight,
            },
            {
                x: GAP * 2 + (PAGE_WIDTH - GAP * 3) / 2,
                y: topHeight + GAP,
                width: (PAGE_WIDTH - GAP * 3) / 2,
                height: bottomHeight,
            },
        ];
    }
    if (count === 4) {
        return Array.from({ length: 4 }).map((_, idx) => {
            const row = Math.floor(idx / 2);
            const col = idx % 2;
            const cellW = Math.floor((PAGE_WIDTH - GAP * 3) / 2);
            const cellH = Math.floor((PAGE_HEIGHT - GAP * 3) / 2);
            return {
                x: GAP + col * (cellW + GAP),
                y: GAP + row * (cellH + GAP),
                width: cellW,
                height: cellH,
            };
        });
    }
    const rows = Math.ceil(Math.sqrt(count));
    const cols = Math.ceil(count / rows);
    const cellW = Math.floor((PAGE_WIDTH - GAP * (cols + 1)) / cols);
    const cellH = Math.floor((PAGE_HEIGHT - GAP * (rows + 1)) / rows);
    const rects = [];
    let idx = 0;
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            if (idx >= count) break;
            rects.push({
                x: GAP + c * (cellW + GAP),
                y: GAP + r * (cellH + GAP),
                width: cellW,
                height: cellH,
            });
            idx++;
        }
    }
    return rects;
}

function buildBubbleText(panel) {
    const dialogue = (panel.dialogue || "").toString().trim();
    if (!dialogue) return "";
    const speaker = (panel.speaker || "").toString().trim();
    return speaker ? `${speaker}: ${dialogue}` : dialogue;
}

function wrapTextCanvas(ctx, text, maxWidth) {
    const words = text.split(/\s+/);
    const lines = [];
    let current = "";
    for (const word of words) {
        const tentative =
            current.length === 0 ? word : `${current} ${word}`.trim();
        if (ctx.measureText(tentative).width > maxWidth && current) {
            lines.push(current);
            current = word;
        } else {
            current = tentative;
        }
    }
    if (current) lines.push(current);
    return lines;
}

function drawSpeechBubble(ctx, { text, x, y, width, height }) {
    const bubbleHeight = Math.min(height, Math.max(80, height * 0.45));
    const padding = 14;
    const radius = 14;
    const tailWidth = 22;
    const tailHeight = 16;

    ctx.save();
    ctx.lineWidth = 2;
    ctx.fillStyle = "#ffffff";
    ctx.strokeStyle = "#000000";
    ctx.font = "20px 'Arial'";

    const maxLineWidth = width - padding * 2;
    const lines = wrapTextCanvas(ctx, text, maxLineWidth);
    const lineHeight = 24;
    const textHeight = lines.length * lineHeight;
    const bodyHeight = Math.min(
        bubbleHeight - tailHeight,
        textHeight + padding * 2
    );

    const rectHeight = Math.max(bodyHeight, 60);
    const rectY = y;
    const rectX = x;

    ctx.beginPath();
    ctx.moveTo(rectX + radius, rectY);
    ctx.lineTo(rectX + width - radius, rectY);
    ctx.quadraticCurveTo(rectX + width, rectY, rectX + width, rectY + radius);
    ctx.lineTo(rectX + width, rectY + rectHeight - radius);
    ctx.quadraticCurveTo(
        rectX + width,
        rectY + rectHeight,
        rectX + width - radius,
        rectY + rectHeight
    );
    ctx.lineTo(rectX + width * 0.35, rectY + rectHeight);
    ctx.lineTo(rectX + width * 0.35 - tailWidth, rectY + rectHeight + tailHeight);
    ctx.lineTo(rectX + width * 0.35 - tailWidth * 1.2, rectY + rectHeight);
    ctx.lineTo(rectX + radius, rectY + rectHeight);
    ctx.quadraticCurveTo(rectX, rectY + rectHeight, rectX, rectY + rectHeight - radius);
    ctx.lineTo(rectX, rectY + radius);
    ctx.quadraticCurveTo(rectX, rectY, rectX + radius, rectY);
    ctx.closePath();

    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = "#000000";
    ctx.textBaseline = "top";
    lines.forEach((line, idx) => {
        ctx.fillText(line, rectX + padding, rectY + padding + idx * lineHeight);
    });

    ctx.restore();
}

async function renderWithCanvas({ panels }) {
    const canvasLib = await loadCanvasLib();
    if (!canvasLib) return null;

    const { createCanvas, loadImage } = canvasLib;
    const canvas = createCanvas(PAGE_WIDTH, PAGE_HEIGHT);
    const ctx = canvas.getContext("2d");

    ctx.fillStyle = "#f7f7f5";
    ctx.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

    const layout = layoutForPanels(panels.length);

    for (let i = 0; i < panels.length; i++) {
        const cell = layout[i] || layout[layout.length - 1];
        const img = await loadImage(panels[i].imageBuffer);
        ctx.drawImage(img, cell.x, cell.y, cell.width, cell.height);

        const text = buildBubbleText(panels[i]);
        if (text) {
            drawSpeechBubble(ctx, {
                text,
                x: cell.x + 10,
                y: cell.y + 10,
                width: cell.width - 20,
                height: Math.floor(cell.height * 0.45),
            });
        }
    }

    return canvas.toBuffer("image/png");
}

function wrapTextFallback(text, maxChars) {
    if (!text) return [""];
    const words = text.split(/\s+/);
    const lines = [];
    let current = "";
    for (const w of words) {
        if ((current + " " + w).trim().length > maxChars) {
            if (current) lines.push(current.trim());
            current = w;
        } else {
            current = `${current} ${w}`.trim();
        }
    }
    if (current) lines.push(current.trim());
    return lines.length ? lines : [""];
}

function speechBubbleSvg({ text, width, height }) {
    const padding = 12;
    const tailHeight = 14;
    const fontSize = 18;
    const safeWidth = Math.max(220, Math.min(width, 520));
    const maxChars = Math.max(14, Math.floor((safeWidth - padding * 2) / 8));
    const lines = wrapTextFallback(text, maxChars);
    const lineHeight = fontSize + 6;
    const bodyHeight = padding * 2 + lines.length * lineHeight;
    const bubbleHeight = Math.min(bodyHeight + tailHeight, height);
    const textYStart = padding + fontSize;
    const textNodes = lines
        .map(
            (line, idx) =>
                `<text x="${padding}" y="${
                    textYStart + idx * lineHeight
                }" font-size="${fontSize}" font-family="Arial, sans-serif" fill="#000">${line.replace(
                    /&/g,
                    "&amp;"
                )}</text>`
        )
        .join("");

    return `
<svg xmlns="http://www.w3.org/2000/svg" width="${safeWidth}" height="${bubbleHeight}">
  <path d="M8 8 h ${safeWidth - 16} a8 8 0 0 1 8 8 v ${bubbleHeight - tailHeight - 16} a8 8 0 0 1 -8 8 h -${
        safeWidth - 16
    } a8 8 0 0 1 -8 -8 v -${bubbleHeight - tailHeight - 16} a8 8 0 0 1 8 -8 z" fill="white" stroke="black" stroke-width="2" />
  <path d="M${safeWidth * 0.18} ${bubbleHeight - tailHeight} l 18 ${
        tailHeight - 2
    } l -8 -${tailHeight}" fill="white" stroke="black" stroke-width="2" />
  ${textNodes}
</svg>
    `.trim();
}

async function renderWithSharp({ panels }) {
    const layout = layoutForPanels(panels.length);
    const composites = [];

    panels.forEach((panel, idx) => {
        const cell = layout[idx] || layout[layout.length - 1];
        composites.push({
            input: sharp(panel.imageBuffer)
                .resize(cell.width, cell.height, { fit: "cover" })
                .png()
                .toBuffer(),
            left: Math.round(cell.x),
            top: Math.round(cell.y),
        });

        const bubble = buildBubbleText(panel);
        if (bubble) {
            composites.push({
                input: Promise.resolve(
                    Buffer.from(
                        speechBubbleSvg({
                            text: bubble,
                            width: cell.width - 20,
                            height: Math.floor(cell.height * 0.45),
                        })
                    )
                ),
                left: Math.round(cell.x + 10),
                top: Math.round(cell.y + 10),
            });
        }
    });

    const resolved = await Promise.all(
        composites.map(async (c) => ({
            input: await c.input,
            left: c.left,
            top: c.top,
        }))
    );

    return sharp({
        create: {
            width: PAGE_WIDTH,
            height: PAGE_HEIGHT,
            channels: 4,
            background: { r: 248, g: 248, b: 246, alpha: 1 },
        },
    })
        .composite(resolved)
        .png()
        .toBuffer();
}

export async function renderComicPage({ storyId, pageIndex, panels }) {
    const canvasBuffer = await renderWithCanvas({ panels });
    if (canvasBuffer) return canvasBuffer;

    logger.info(
        { storyId, pageIndex },
        "Rendered with sharp fallback for story page"
    );
    return renderWithSharp({ panels });
}
