# Stage 1: Install dependencies and build standalone application
FROM node:20-alpine AS builder

WORKDIR /app

# Enable corepack and prepare pnpm
RUN corepack enable && corepack prepare pnpm@12.15.0 --activate

# Copy package manifests
COPY package.json pnpm-lock.yaml ./

# Install dependencies using frozen lockfile
RUN pnpm install --frozen-lockfile

# Copy source code
COPY . .

# Build Next.js application (generates .next/standalone)
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm run build

# Stage 2: Minimal Node.js runtime
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME="0.0.0.0"

# Create non-root system user and group
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy standalone output and static assets
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=3 \
    CMD node -e "require('http').get('http://127.0.0.1:3000/', (res) => { process.exit(res.statusCode === 200 ? 0 : 1); }).on('error', () => process.exit(1));"

CMD ["node", "server.js"]
