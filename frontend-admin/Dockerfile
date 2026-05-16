FROM node:22-alpine AS build

WORKDIR /app

RUN npm install -g pnpm@10.5.0

COPY frontend-admin/package.json frontend-admin/pnpm-lock.yaml frontend-admin/pnpm-workspace.yaml ./
COPY frontend-admin/packages ./packages
RUN pnpm install --frozen-lockfile

COPY frontend-admin ./

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN pnpm build

FROM node:22-alpine

WORKDIR /app

RUN npm install -g pnpm@10.5.0

COPY --from=build /app/package.json ./package.json
COPY --from=build /app/pnpm-lock.yaml ./pnpm-lock.yaml
COPY --from=build /app/pnpm-workspace.yaml ./pnpm-workspace.yaml
COPY --from=build /app/packages ./packages
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist

EXPOSE 3001

CMD ["pnpm", "exec", "vite", "preview", "--host", "0.0.0.0", "--port", "3001"]
