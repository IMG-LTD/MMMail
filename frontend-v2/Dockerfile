FROM node:20-alpine AS build

WORKDIR /app

RUN npm install -g pnpm@9.15.5

COPY frontend-v2/package.json frontend-v2/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY frontend-v2 .

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN pnpm build

FROM node:20-alpine

WORKDIR /app

RUN npm install -g pnpm@9.15.5

COPY --from=build /app/package.json ./package.json
COPY --from=build /app/pnpm-lock.yaml ./pnpm-lock.yaml
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist

EXPOSE 3001

CMD ["pnpm", "exec", "vite", "preview", "--host", "0.0.0.0", "--port", "3001"]
