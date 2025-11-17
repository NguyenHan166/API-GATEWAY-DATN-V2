// Example test file for AI Beautify API
// Run with: node src/features/aiBeautify/example.test.js

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Example usage with fetch
async function testAIBeautify() {
    const API_BASE = "http://localhost:3000/api";

    // Read test image
    const imagePath = path.join(__dirname, "test-image.jpg");
    if (!fs.existsSync(imagePath)) {
        console.error("Please add a test-image.jpg file to this directory");
        return;
    }

    const imageBuffer = fs.readFileSync(imagePath);

    // Create form data
    const formData = new FormData();
    const blob = new Blob([imageBuffer], { type: "image/jpeg" });
    formData.append("image", blob, "test.jpg");

    try {
        console.log("🚀 Sending request to AI Beautify...");
        const startTime = Date.now();

        const response = await fetch(`${API_BASE}/ai-beautify`, {
            method: "POST",
            body: formData,
        });

        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`⏱️  Processing time: ${duration}s`);

        if (!response.ok) {
            const error = await response.json();
            console.error("❌ Error:", error);
            return;
        }

        const result = await response.json();
        console.log("✅ Success!");
        console.log(JSON.stringify(result, null, 2));

        // Download enhanced image
        if (result.data?.presignedUrl) {
            console.log("\n📥 Downloading enhanced image...");
            const imageResponse = await fetch(result.data.presignedUrl);
            const enhancedBuffer = await imageResponse.arrayBuffer();

            const outputPath = path.join(__dirname, "test-image-enhanced.jpg");
            fs.writeFileSync(outputPath, Buffer.from(enhancedBuffer));
            console.log(`✅ Saved to: ${outputPath}`);
        }
    } catch (error) {
        console.error("❌ Request failed:", error.message);
    }
}

// Example with cURL command
function printCurlExample() {
    console.log("\n📋 cURL Example:");
    console.log(
        `
curl -X POST http://localhost:3000/api/ai-beautify \\
  -F "image=@./path/to/your/image.jpg" \\
  -H "Accept: application/json"
    `.trim()
    );
}

// Example response
function printExampleResponse() {
    console.log("\n📋 Example Response:");
    console.log(
        `
{
  "success": true,
  "requestId": "abc123xyz",
  "data": {
    "key": "aiBeautify/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
    "url": "https://your-domain.com/aiBeautify/2025-11-18/550e8400-e29b-41d4-a716-446655440000.jpg",
    "presignedUrl": "https://r2.cloudflarestorage.com/bucket/...",
    "expiresIn": 3600,
    "meta": {
      "bytes": 245678,
      "requestId": "abc123xyz",
      "pipeline": [
        "pre-scale",
        "gfpgan",
        "real-esrgan",
        "skin-retouch",
        "tone-enhance"
      ]
    }
  }
}
    `.trim()
    );
}

// Run examples
if (import.meta.url === `file://${process.argv[1]}`) {
    console.log("🎨 AI Beautify API Test\n");
    printCurlExample();
    printExampleResponse();
    console.log("\n");
    // Uncomment to run actual test:
    // testAIBeautify();
}

export { testAIBeautify };
