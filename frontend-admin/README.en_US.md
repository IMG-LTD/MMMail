# MMMail Frontend Admin

`frontend-admin` is the current MMMail product frontend. It owns the admin/workspace runtime, public boundary pages, e2e, coverage, bundle, i18n, and style discipline gates for v2.1.x and later work.

## Role

- Product frontend: new product, commercial, and enterprise access work targets `frontend-admin`.
- Stack: Vue 3, Vite, TypeScript, Naive UI, UnoCSS, Pinia, Vue Router.
- Runtime entry: published through the repository self-hosting docs and Compose/Docker paths.
- Legacy reference: `frontend-v2` is kept only as legacy contract / migration reference.

## Common Commands

```bash
pnpm install
pnpm dev
pnpm typecheck
pnpm test:v212
pnpm test:coverage
pnpm test:e2e
pnpm check:style-discipline
pnpm check:i18n
pnpm check:bundle-budget
```

## API Type Generation

```bash
MMMAIL_OPENAPI_SOURCE=../contracts/openapi/v21-api-catalog.yaml pnpm gen:api
```

Generated files must keep a clean diff.

## Attribution

`frontend-admin` derived from Soybean Admin and keeps the upstream MIT license in `LICENSE`. Repository-level attribution is recorded in `../NOTICE`.
