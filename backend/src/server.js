import { createApp } from "./app.js";
import { env } from "./config/env.js";
import { logger } from "./config/logger.js";

const app = createApp();
const server = app.listen(Number(env.PORT), () =>
    logger.info(`Server listening on :${env.PORT}`)
);

// Graceful shutdown
function shutdown(sig) {
    logger.info({ sig }, "Shutting down...");
    server.close((err) => {
        if (err) {
            logger.error({ err }, "Error closing server");
            process.exit(1);
        }
        logger.info("Server closed. Bye.");
        process.exit(0);
    });
    // Force kill sau 10s nếu treo
    setTimeout(() => process.exit(1), 10_000).unref();
}

process.on("SIGINT", () => shutdown("SIGINT"));
process.on("SIGTERM", () => shutdown("SIGTERM"));
