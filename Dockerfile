# Chọn base image Node 20
FROM node:20-bullseye

# Cài font hỗ trợ tiếng Việt + fontconfig cho Sharp/libvips
RUN apt-get update && \
    apt-get install -y \
        fonts-noto \
        fonts-noto-cjk \
        fonts-dejavu-core \
        fontconfig && \
    rm -rf /var/lib/apt/lists/*

# Rebuild font cache
RUN fc-cache -f -v

# Thư mục làm việc
WORKDIR /app

# Copy file package và cài dependency
COPY package*.json ./
RUN npm ci --omit=dev

# Copy toàn bộ source
COPY . .

# Port app lắng nghe (theo env.PORT, mặc định 3000)
EXPOSE 3000

# Lệnh start
CMD ["npm", "start"]
