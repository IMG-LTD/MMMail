# 后端 v2.1 API Contract Runtime 收口执行计划

> 使用 `superpowers-zh:writing-plans` 编写。目标是让后端运行时 contract catalog、OpenAPI catalog 与前端 v2.1 API client 命名空间完成闭环，并保持失败显式暴露。

## 目标

完成 `backend-v21-api-contract-runtime-closure` 切片：

- `/api/v2/platform/contracts` 运行时 catalog 覆盖前端 v2.1 已声明的 API 家族。
- 补齐 billing 与 entitlements 命名空间，避免前端 v2.1 contract test 已存在而后端 catalog 缺失。
- OpenAPI catalog 与 Java runtime catalog 对 billing、entitlements 的 owner、permission、entitlement、design source 保持一致。
- 不添加真实业务接口实现，不返回模拟业务成功响应，不绕过鉴权。
- 开发前记录当前进度，开发完成后更新进度。

## 当前上下文

已存在后端 contract 结构：

- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContract.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractMetadata.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiAccess.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiOwner.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiSchema.java`
- `backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21ApiContractCatalogController.java`
- `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`
- `contracts/openapi/v21-api-catalog.yaml`

当前 runtime catalog 已覆盖 workspace、mail、calendar、drive、docs、sheets、labs、pass、collaboration、command-center、notifications、admin-governance、settings、identity、public-share、system。缺口是 billing 与 entitlements。

前端 v2.1 已声明的 billing 与 entitlements API：

- `GET /api/v2/billing/summary`
- `GET /api/v2/billing/plans`
- `GET /api/v2/billing/invoices`
- `GET /api/v2/billing/usage`
- `GET /api/v2/entitlements`
- `GET /api/v2/entitlements/matrix`

## 文件改动计划

### 1. 进度文件记录活动切片

编辑 `docs/superpowers/progress/v21-implementation-progress.md`。

在 `## Remaining Risks` 之前插入：

```markdown
## Active Slice

- Slice: `backend-v21-api-contract-runtime-closure`
- Status: `in_progress`
- Started: `2026-05-12`
- Scope: backend runtime API contract catalog, OpenAPI catalog, backend catalog tests, progress tracking
- Verification target: `BackendV21ApiContractCatalogTest`, backend compile, frontend v2.1 contract suite
```

不删除已完成前端切片记录。

### 2. 先写失败测试

编辑 `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`。

新增 import：

```java
import java.util.List;
import java.util.Set;
```

在 class 常量区加入：

```java
    private static final Set<String> REQUIRED_OWNER_MODULES = Set.of(
            "workspace",
            "mail",
            "calendar",
            "drive",
            "docs",
            "sheets",
            "labs",
            "pass",
            "collaboration",
            "command-center",
            "notifications",
            "admin-governance",
            "billing",
            "entitlements",
            "settings",
            "identity",
            "public-share",
            "system"
    );
```

新增测试方法：

```java
    @Test
    void catalogShouldCoverBackendRuntimeFamiliesRequiredByFrontendV21() {
        V21ApiContractCatalog catalog = V21ApiContractCatalog.defaultCatalog();
        Map<String, V21ApiContract> contractsByIdentity = catalog.contracts().stream()
                .collect(Collectors.toMap(V21ApiContract::identity, Function.identity()));
        Set<String> ownerModules = catalog.contracts().stream()
                .map(V21ApiContract::ownerModule)
                .collect(Collectors.toSet());

        assertThat(ownerModules).containsAll(REQUIRED_OWNER_MODULES);
        assertContract(contractsByIdentity, "GET /api/v2/billing/summary", "billing", "BillingSummary", "hosted");
        assertContract(contractsByIdentity, "GET /api/v2/billing/plans", "billing", "BillingPlan[]", "hosted");
        assertContract(contractsByIdentity, "GET /api/v2/billing/invoices", "billing", "BillingInvoice[]", "hosted");
        assertContract(contractsByIdentity, "GET /api/v2/billing/usage", "billing", "BillingUsage", "hosted");
        assertContract(contractsByIdentity, "GET /api/v2/entitlements", "entitlements", "EntitlementState[]", "community");
        assertContract(contractsByIdentity, "GET /api/v2/entitlements/matrix", "entitlements", "EntitlementMatrix", "community");

        for (V21ApiContract contract : catalog.contracts()) {
            assertContractMetadata(contract);
        }
    }
```

新增 helper：

```java
    private void assertContractMetadata(V21ApiContract contract) {
        assertThat(contract.method()).isIn("GET", "POST", "PATCH", "DELETE");
        assertThat(contract.path()).startsWith("/api/v2/");
        assertThat(contract.ownerModule()).isNotBlank();
        assertThat(contract.responseModel()).isNotBlank();
        assertThat(contract.permissions()).isNotEmpty();
        assertThat(contract.permissions()).allSatisfy(permission -> assertThat(permission).isNotBlank());
        assertThat(contract.entitlement()).isIn("community", "premium", "hosted", "enterprise-governance");
        assertThat(contract.designSource()).startsWith("docs/MMMail/UI/");
    }
```

修改已有 endpoint payload 断言：

```java
        assertThat(contracts).hasSize(122);
```

修改已有 OpenAPI 断言，在 `openApiCatalogShouldContainSameGovernanceMetadata()` 内增加：

```java
        assertThat(yaml).contains("/api/v2/billing/summary:");
        assertThat(yaml).contains("x-permission: [\"billing:read\"]");
        assertThat(yaml).contains("x-entitlement: hosted");
        assertThat(yaml).contains("/api/v2/entitlements:");
        assertThat(yaml).contains("x-permission: [\"entitlements:read\"]");
```

运行红灯验证：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest
```

预期：命令退出非 0，失败原因指向缺少 billing、entitlements owner 或 endpoint 数量仍为 116。

### 3. 补齐 Java runtime catalog

编辑 `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`。

在 `defaultCatalog()` 的 `Stream.of(...)` 中，将 billing 与 entitlements 放在 `adminGovernanceContracts()` 之后、`settingsContracts()` 之前：

```java
                adminGovernanceContracts(),
                billingContracts(),
                entitlementsContracts(),
                settingsContracts(),
```

新增方法：

```java
    private static List<V21ApiContract> billingContracts() {
        return module("billing", "docs/MMMail/UI/Admin", new String[][]{
                {"GET", "/api/v2/billing/summary", "BillingSummary", HOSTED, "billing:read"},
                {"GET", "/api/v2/billing/plans", "BillingPlan[]", HOSTED, "billing:plans:read"},
                {"GET", "/api/v2/billing/invoices", "BillingInvoice[]", HOSTED, "billing:invoices:read"},
                {"GET", "/api/v2/billing/usage", "BillingUsage", HOSTED, "billing:usage:read"}
        });
    }

    private static List<V21ApiContract> entitlementsContracts() {
        return module("entitlements", "docs/MMMail/UI/Admin", new String[][]{
                {"GET", "/api/v2/entitlements", "EntitlementState[]", COMMUNITY, "entitlements:read"},
                {"GET", "/api/v2/entitlements/matrix", "EntitlementMatrix", COMMUNITY, "entitlements:matrix:read"}
        });
    }
```

不修改 controller，不添加业务 controller，不绕过 Spring Security。

### 4. 补齐 OpenAPI catalog

编辑 `contracts/openapi/v21-api-catalog.yaml`。

在 admin-governance 与 settings 区域之间插入：

```yaml
  /api/v2/billing/summary:
    get: {summary: Read billing summary, x-permission: ["billing:read"], x-entitlement: hosted, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/billing/plans:
    get: {summary: List billing plans, x-permission: ["billing:plans:read"], x-entitlement: hosted, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/billing/invoices:
    get: {summary: List billing invoices, x-permission: ["billing:invoices:read"], x-entitlement: hosted, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/billing/usage:
    get: {summary: Read billing usage, x-permission: ["billing:usage:read"], x-entitlement: hosted, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/entitlements:
    get: {summary: List entitlement states, x-permission: ["entitlements:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/entitlements/matrix:
    get: {summary: Read entitlement matrix, x-permission: ["entitlements:matrix:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
```

保持 YAML 文件小于 500 行。

### 5. 绿灯验证

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest
```

预期：退出码 0，Surefire 输出包含 `Failures: 0` 与 `Errors: 0`。

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile
```

预期：退出码 0，Maven 输出 `BUILD SUCCESS`。

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
```

预期：退出码 0，Vitest 输出测试全部通过。

### 6. 提交实现切片

执行：

```bash
git status --short --branch
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java
git add contracts/openapi/v21-api-catalog.yaml
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "test(backend-v21): close API contract runtime coverage"
```

预期：提交成功，并得到一个新的本地 commit hash。

### 7. 开发完成后更新进度

编辑 `docs/superpowers/progress/v21-implementation-progress.md`。

移除 `## Active Slice` 区块，增加或更新完成记录：

```markdown
## Latest Completed Slice

- Slice: `backend-v21-api-contract-runtime-closure`
- Status: `completed`
- Completed: `2026-05-12`
- Commit: `实际实现提交 hash test(backend-v21): close API contract runtime coverage`
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest`
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile`
  - `timeout 60s pnpm --dir frontend-v2 test`
```

在 completed slices 列表中加入一行：

```markdown
| Backend API contract runtime closure | `backend-v21-api-contract-runtime-closure` | completed | `BackendV21ApiContractCatalogTest`; `contracts/openapi/v21-api-catalog.yaml`; `/api/v2/platform/contracts` |
```

提交进度更新：

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update API contract runtime progress"
git status --short --branch
```

## 最终验收标准

- `V21ApiContractCatalog.defaultCatalog()` 包含 billing 与 entitlements owner。
- `/api/v2/platform/contracts` 的 `contracts` 数量为 122。
- 后端测试覆盖新增命名空间、metadata 完整性、匿名访问拒绝、OpenAPI governance metadata。
- OpenAPI YAML 包含新增 6 个 path，且 permission、entitlement、design source 与 Java catalog 一致。
- 进度文档记录开发前 `in_progress` 与开发后 `completed` 状态。
- 所有相关验证命令均退出 0 后再提交实现与进度。

## 自检清单

- 没有添加 mock、fake success、silent fallback。
- 没有添加真实业务 controller 或绕过鉴权。
- 没有修改 `/api/v1`。
- 没有把 `.superpowers/`、`.tmp/`、`frontend/`、压缩包或未跟踪目录加入提交。
- Java 文件低于 500 行，新增函数低于 50 行，嵌套深度不超过 3。
- 提交只包含本切片相关文件。
