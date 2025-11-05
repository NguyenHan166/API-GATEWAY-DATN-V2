import Replicate from "replicate";
import { env } from "../../config/env.js";

export const replicate = new Replicate({ auth: env.REPLICATE_API_TOKEN });
