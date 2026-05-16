# MMMail Frontend Admin

`frontend-admin` 是 MMMail 当前产品前端，承载 v2.1.x 之后的管理台、工作台、公开边界页面、e2e、coverage、bundle、i18n 和 style discipline gate。

## 定位

- 唯一产品前端：新功能、新商业化入口、新企业准入入口只进入 `frontend-admin`。
- 技术栈：Vue 3、Vite、TypeScript、Naive UI、UnoCSS、Pinia、Vue Router。
- 运行入口：随主仓 Compose / Docker / 自托管文档发布。
- 旧契约来源：`frontend-v2` 只作为 legacy contract / migration reference。

## 常用命令

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

## API 类型生成

```bash
MMMAIL_OPENAPI_SOURCE=../contracts/openapi/v21-api-catalog.yaml pnpm gen:api
```

生成结果必须保持 clean diff。

## Attribution

`frontend-admin` derived from Soybean Admin and keeps the upstream MIT license in `LICENSE`. Repository-level attribution is recorded in `../NOTICE`.
