import pino from "pino";
import { env } from "./env.js";

const isProd = env.NODE_ENV === "production";

export const logger = isProd
    ? pino({ level: "info" })
    : pino({
          level: "debug",
          transport: {
              target: "pino-pretty",
              options: {
                  colorize: true, // màu sắc đẹp hơn
                  translateTime: "SYS:standard", // hiển thị thời gian dễ đọc
                  ignore: "pid,hostname", // ẩn bớt field
              },
          },
      });
