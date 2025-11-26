import sharp from "sharp";
import { logger } from "../../config/logger.js";

const PAGE_WIDTH = 1080;
const PAGE_HEIGHT = 1620;
const GAP = 24;

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

function escapeXml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&apos;");
}

function wrapTextFallback(text, maxChars) {
    if (!text) return [""];

    // Tối ưu cho tiếng Việt: tách theo từ, không tách giữa chữ
    const words = text.split(/\s+/);
    const lines = [];
    let current = "";

    for (const word of words) {
        const testLine = current ? `${current} ${word}` : word;

        // Ước lượng độ rộng: tiếng Việt có dấu nên cần nhiều space hơn
        if (testLine.length > maxChars && current) {
            lines.push(current.trim());
            current = word;
        } else {
            current = testLine;
        }
    }

    if (current.trim()) {
        lines.push(current.trim());
    }

    return lines.length > 0 ? lines : [""];
}

function speechBubbleSvg({ text, width, height }) {
    const padding = 14;
    const tailHeight = 16;
    const fontSize = 20;
    const safeWidth = Math.max(240, Math.min(width, 540));

    // Tính toán maxChars dựa trên font size và width thực tế
    const avgCharWidth = fontSize * 0.5; // Ước lượng độ rộng trung bình của 1 ký tự
    const maxChars = Math.floor((safeWidth - padding * 2) / avgCharWidth);

    const lines = wrapTextFallback(text, Math.max(15, maxChars));
    const lineHeight = fontSize + 8;
    const bodyHeight = padding * 2 + lines.length * lineHeight + 4;
    const bubbleHeight = Math.min(bodyHeight + tailHeight, height);
    const textYStart = padding + 4;

    // Generate unique ID for filter to avoid conflicts
    const filterId = `shadow-${Math.random().toString(36).substr(2, 9)}`;

    // Tạo text nodes với XML escape và styling tốt hơn
    const textNodes = lines
        .map(
            (line, idx) =>
                `<text 
                    x="${padding + 2}" 
                    y="${textYStart + idx * lineHeight}" 
                    font-size="${fontSize}" 
                    font-family="'Noto Sans', 'Segoe UI', Arial, sans-serif" 
                    font-weight="600"
                    fill="#1a1a1a"
                    dominant-baseline="hanging"
                    xml:space="preserve">${escapeXml(line)}</text>`
        )
        .join("\n    ");

    return `
<svg xmlns="http://www.w3.org/2000/svg" width="${safeWidth}" height="${bubbleHeight}">
  <defs>
    <filter id="${filterId}" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur in="SourceAlpha" stdDeviation="3"/>
      <feOffset dx="2" dy="3" result="offsetblur"/>
      <feComponentTransfer>
        <feFuncA type="linear" slope="0.2"/>
      </feComponentTransfer>
      <feMerge>
        <feMergeNode/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>
  
  <!-- Bubble shadow -->
  <path 
    d="M10 0 h ${safeWidth - 20} a10 10 0 0 1 10 10 v ${
        bubbleHeight - tailHeight - 20
    } a10 10 0 0 1 -10 10 h -${safeWidth - 20} a10 10 0 0 1 -10 -10 v -${
        bubbleHeight - tailHeight - 20
    } a10 10 0 0 1 10 -10 z" 
    fill="rgba(0,0,0,0.08)" 
    transform="translate(3,4)" />
  
  <!-- Bubble body -->
  <path 
    d="M10 0 h ${safeWidth - 20} a10 10 0 0 1 10 10 v ${
        bubbleHeight - tailHeight - 20
    } a10 10 0 0 1 -10 10 h -${safeWidth - 20} a10 10 0 0 1 -10 -10 v -${
        bubbleHeight - tailHeight - 20
    } a10 10 0 0 1 10 -10 z" 
    fill="white" 
    stroke="#2c2c2c" 
    stroke-width="2.5" 
    stroke-linejoin="round"
    filter="url(#${filterId})" />
  
  <!-- Tail shadow -->
  <path 
    d="M${safeWidth * 0.15} ${bubbleHeight - tailHeight} l 20 ${
        tailHeight - 2
    } l -10 -${tailHeight + 2}" 
    fill="rgba(0,0,0,0.08)" 
    transform="translate(3,4)" />
  
  <!-- Tail -->
  <path 
    d="M${safeWidth * 0.15} ${bubbleHeight - tailHeight} l 20 ${
        tailHeight - 2
    } l -10 -${tailHeight + 2}" 
    fill="white" 
    stroke="#2c2c2c" 
    stroke-width="2.5" 
    stroke-linejoin="round" />
  
  <!-- Text content -->
  <g>
    ${textNodes}
  </g>
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
    logger.info(
        { storyId, pageIndex, panelCount: panels.length },
        "Rendering comic page with Sharp + optimized SVG bubbles"
    );
    return renderWithSharp({ panels });
}
