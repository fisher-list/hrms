# ============================================================
# Frontend multi-stage build — Vue 3 + Vite + Nginx
# ============================================================

# ---- Stage 1: Build ----
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies first (layer cache)
COPY package.json package-lock.json* yarn.lock* pnpm-lock.yaml* ./
RUN if [ -f pnpm-lock.yaml ]; then \
        corepack enable && pnpm install --frozen-lockfile; \
    elif [ -f yarn.lock ]; then \
        yarn install --frozen-lockfile; \
    else \
        npm ci || npm install; \
    fi

# Copy source & build
COPY . .
RUN npm run build

# ---- Stage 2: Nginx ----
FROM nginx:alpine AS runtime
LABEL maintainer="hrms-team"

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our nginx config
COPY --from=builder /app/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
