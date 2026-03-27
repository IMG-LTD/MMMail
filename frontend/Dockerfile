FROM node:20-alpine AS build

WORKDIR /app

RUN npm install -g pnpm@9.15.5

COPY frontend/package.json frontend/pnpm-lock.yaml ./

RUN pnpm install --frozen-lockfile --ignore-scripts

COPY frontend .

ARG NUXT_PUBLIC_API_BASE=http://localhost:8080
ARG NUXT_PUBLIC_AUTH_CSRF_COOKIE_NAME=MMMAIL_CSRF_TOKEN
ARG NUXT_PUBLIC_ENABLE_PREVIEW_MODULES=false

ENV NUXT_PUBLIC_API_BASE=$NUXT_PUBLIC_API_BASE
ENV NUXT_PUBLIC_AUTH_CSRF_COOKIE_NAME=$NUXT_PUBLIC_AUTH_CSRF_COOKIE_NAME
ENV NUXT_PUBLIC_ENABLE_PREVIEW_MODULES=$NUXT_PUBLIC_ENABLE_PREVIEW_MODULES

RUN pnpm prepare && pnpm build

FROM node:20-alpine

WORKDIR /app

ENV HOST=0.0.0.0
ENV PORT=3001
ENV NODE_ENV=production

COPY --from=build /app/.output ./.output

EXPOSE 3001

CMD ["node", ".output/server/index.mjs"]
