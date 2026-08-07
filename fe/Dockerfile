# ── 빌드 단계 ───────────────────────────────────────────────
FROM node:22-alpine AS build

RUN corepack enable

WORKDIR /app

# 잠금 파일까지 먼저 복사해야 의존성 레이어가 캐시된다.
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

# ── 실행 단계 ───────────────────────────────────────────────
# 빌드 결과는 정적 파일이므로 Node를 띄울 이유가 없다. nginx가 훨씬 가볍다.
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1
