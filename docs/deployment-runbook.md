# 部署与运行手册（v100）

**版本**: `v100.0`  
**日期**: `2026-03-11`  
**作者**: `Codex`  
**主题**: `2FA grace period / blocked → setup → restore`

## 1. 本地启动顺序

### 1.1 后端
在仓库根目录执行：

- `./scripts/start-backend-local.sh`

等价手动命令：

- `cd backend && timeout 60s mvn -pl mmmail-common -am -DskipTests install`
- `cd backend/mmmail-server && mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles=local`

健康检查：

- `curl -sf http://127.0.0.1:8080/actuator/health`

关键说明：

- `mmmail-server` 依赖 `mmmail-common` 的本地 `SNAPSHOT`。
- 若直接在 `backend/mmmail-server` 执行 `mvn spring-boot:run`，但本地 Maven 仓库里的 `mmmail-common` 仍是旧版本，会触发 `NoSuchFieldError: ErrorCode.ORG_TWO_FACTOR_REQUIRED`。
- 因此，本地启动前必须先安装最新 `mmmail-common`，不能只做 `compile`。

### 1.2 前端开发模式
在仓库根目录执行：

- `cd frontend`
- `pnpm install`
- `pnpm dev --host 127.0.0.1 --port 3001`

访问地址：

- `http://127.0.0.1:3001/inbox`

### 1.3 前端 UAT / Playwright 模式
本轮 Playwright 使用生产构建产物验证：

- `cd frontend && pnpm build`
- `cd frontend && PORT=3001 HOST=127.0.0.1 node .output/server/index.mjs`

关键说明：

- 若 `3001` 端口仍被旧 Nuxt 进程占用，浏览器可能打到旧前端代码。
- 进行 UAT 前必须先确认旧进程已释放。

## 2. 验证命令

### 2.1 后端
- `cd backend && timeout 60s mvn -pl mmmail-server -am -Dtest=OrgAuthenticationSecurityIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`

### 2.2 前端
- `cd frontend && pnpm typecheck`
- `cd frontend && pnpm exec vitest run tests/product-access-blocked.spec.ts tests/authenticator-recovery.spec.ts tests/i18n.spec.ts tests/organizations-auth-security.spec.ts`
- `cd frontend && pnpm build`

## 3. 运行态基线

### 3.1 组织 2FA 阻断接口
当组织启用 `All members` 且 `grace period = 0` 时，组织作用域产品接口应返回：

- HTTP：`403`
- 业务码：`30046`
- 业务消息：`Organization policy requires two-factor authentication before you can continue. Open /authenticator to recover access.`

### 3.2 恢复链路
前端应满足：

- 用户访问组织作用域产品后进入 `/product-access-blocked`
- CTA 显示 `Setup 2FA`
- 点击后跳转 `/authenticator?recovery=ORG_TWO_FACTOR_REQUIRED...`
- Authenticator 先切到 personal scope
- 新建或保存 entry 后自动恢复 org scope 并回跳原产品页面

## 4. 本轮真实问题与规避

### 4.1 后端本地运行态类路径漂移
- 现象：组织作用域访问产品 API 返回 `500 / 90000`
- 根因：`mmmail-server` 运行时加载了旧版 `mmmail-common` `SNAPSHOT`
- 修复：启动前先执行 `mvn -pl mmmail-common -am -DskipTests install`

### 4.2 前端端口打到旧产物
- 现象：浏览器显示旧页面逻辑，Playwright 结果与源码不一致
- 根因：`3001` 仍被旧 Nuxt 进程占用
- 修复：释放旧进程后，用当前 `pnpm build` 产物重新启动

## 5. v100 UAT 完成态基线
通过时应满足：

- `Organizations -> Authentication security` 可显示 `grace period`
- owner 可把 `grace period` 在 `1` 和 `0` 之间切换
- `grace = 1` 时，成员状态显示 `In grace period`
- `grace = 0` 时，成员状态显示 `Blocked by policy`
- member 访问 `/docs` 会进入 `product-access-blocked`
- member 点击 `Setup 2FA` 后进入 recovery 模式的 `/authenticator`
- member 点击 `New entry` 后自动恢复到 `/docs`
