# Backend v2.1 Release Gate Hardening 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将已完成的 `BackendV21*` 后端回归测试纳入本地 release gate 和 GitHub Actions，避免 v2.1 runtime bridge 后续静默回退。

**架构：** 先新增一个前端 Node contract 测试，自动读取仓库中已提交的 `BackendV21*Test.java` 文件，并断言 `scripts/validate-local.sh` 与 `.github/workflows/ci.yml` 都覆盖同一组测试。随后补齐本地验证脚本和 CI backend job，最后更新 v2.1 进度记录。

**技术栈：** Bash、GitHub Actions YAML、Node test runner、Maven Surefire、Spring Boot integration tests、pnpm。

---

## 文件结构

- 创建：`frontend-v2/tests/v21-release-gate-hardening-contract.test.mjs`
  - 职责：冻结 release gate 对所有已提交 `BackendV21*Test` 的覆盖，防止脚本和 CI 再次遗漏 v2.1 回归组。
- 修改：`scripts/validate-local.sh`
  - 职责：新增 `BACKEND_V21_RUNTIME_TESTS` 分组和 `backend v2.1 runtime regression` 本地验证阶段。
- 修改：`.github/workflows/ci.yml`
  - 职责：在 backend job 中加入与本地脚本一致的 `BackendV21*` 回归阶段。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  - 职责：记录本 slice 的提交、验证命令和完成状态。

## 任务 1：新增失败的 release gate contract 测试

**文件：**
- 创建：`frontend-v2/tests/v21-release-gate-hardening-contract.test.mjs`

- [ ] **步骤 1：编写失败的测试**

创建 `frontend-v2/tests/v21-release-gate-hardening-contract.test.mjs`：

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'

const repoRoot = new URL('../../', import.meta.url)
const backendTestDir = new URL('backend/mmmail-server/src/test/java/com/mmmail/server/', repoRoot)
const validateLocalFile = new URL('scripts/validate-local.sh', repoRoot)
const ciFile = new URL('.github/workflows/ci.yml', repoRoot)
const backendV21TestFile = /^BackendV21.*Test\.java$/

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function parseValidateLocalGroup(content) {
  const match = content.match(/BACKEND_V21_RUNTIME_TESTS="([^"]+)"/)
  assert.ok(match, 'validate-local missing BACKEND_V21_RUNTIME_TESTS')
  return match[1].split(',').map(item => item.trim()).filter(Boolean)
}

test('local and CI release gates include every committed BackendV21 regression', async () => {
  const [backendFiles, validateLocal, ci] = await Promise.all([
    readdir(backendTestDir),
    readFile(validateLocalFile, 'utf8'),
    readFile(ciFile, 'utf8')
  ])

  const backendV21Tests = backendFiles
    .filter(fileName => backendV21TestFile.test(fileName))
    .map(fileName => fileName.replace(/\.java$/, ''))
    .sort()

  assert.ok(backendV21Tests.length > 0, 'expected committed BackendV21 tests')
  assert.deepEqual(parseValidateLocalGroup(validateLocal).sort(), backendV21Tests)
  assert.match(validateLocal, /echo "\[validate-local\] backend v2\.1 runtime regression"/)
  assert.match(validateLocal, /-Dtest="\$BACKEND_V21_RUNTIME_TESTS"/)
  assert.match(validateLocal, /\/tmp\/mmmail-backend-v21-runtime\.log/)
  assert.match(ci, /Backend v2\.1 runtime regression/)

  for (const className of backendV21Tests) {
    assert.match(ci, new RegExp(`\\b${escapeRegExp(className)}\\b`), `CI missing ${className}`)
  }
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 exec node --test tests/v21-release-gate-hardening-contract.test.mjs
```

预期：FAIL，输出包含：

```text
validate-local missing BACKEND_V21_RUNTIME_TESTS
```

- [ ] **步骤 3：Commit 失败测试**

```bash
git add frontend-v2/tests/v21-release-gate-hardening-contract.test.mjs
git diff --cached --check
git commit -m "test(backend-v21): cover release gate runtime regressions"
```

## 任务 2：补齐本地脚本和 CI v2.1 回归门禁

**文件：**
- 修改：`scripts/validate-local.sh`
- 修改：`.github/workflows/ci.yml`
- 测试：`frontend-v2/tests/v21-release-gate-hardening-contract.test.mjs`

- [ ] **步骤 1：更新 validate-local 测试分组**

在 `scripts/validate-local.sh` 的 `BACKEND_V2_CONTRACT_TESTS` 后新增：

```bash
BACKEND_V21_RUNTIME_TESTS="BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21BackgroundJobFoundationTest,BackendV21CalendarRuntimeBridgeTest,BackendV21CollaborationWriteRuntimeTest,BackendV21CommunityRuntimeClosureTest,BackendV21DocsSheetsRuntimeBridgeTest,BackendV21DriveRuntimeBridgeTest,BackendV21EventOutboxFoundationTest,BackendV21MailRuntimeBridgeTest,BackendV21OpsRuntimeBridgeTest,BackendV21PassRuntimeBridgeTest,BackendV21RuntimeContractGapClosureTest"
```

- [ ] **步骤 2：新增 validate-local v2.1 回归阶段**

在 `backend v2 contract regression` Maven block 之后新增：

```bash
echo "[validate-local] backend v2.1 runtime regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_V21_RUNTIME_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-v21-runtime.log 2>&1
```

- [ ] **步骤 3：更新 GitHub Actions backend job**

在 `.github/workflows/ci.yml` 的 backend job 中，在 `Backend auth/rbac and v2 contract regression` 步骤后新增：

```yaml
      - name: Backend v2.1 runtime regression
        run: >
          mvn -f backend/pom.xml -pl mmmail-server -am
          -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21BackgroundJobFoundationTest,BackendV21CalendarRuntimeBridgeTest,BackendV21CollaborationWriteRuntimeTest,BackendV21CommunityRuntimeClosureTest,BackendV21DocsSheetsRuntimeBridgeTest,BackendV21DriveRuntimeBridgeTest,BackendV21EventOutboxFoundationTest,BackendV21MailRuntimeBridgeTest,BackendV21OpsRuntimeBridgeTest,BackendV21PassRuntimeBridgeTest,BackendV21RuntimeContractGapClosureTest
          -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **步骤 4：运行 contract 测试验证通过**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 exec node --test tests/v21-release-gate-hardening-contract.test.mjs
```

预期：PASS，输出包含：

```text
# pass 1
# fail 0
```

- [ ] **步骤 5：运行脚本语法检查**

运行：

```bash
bash -n scripts/validate-local.sh
```

预期：退出码 `0`，无输出。

- [ ] **步骤 6：Commit 门禁脚本与 CI**

```bash
git add scripts/validate-local.sh .github/workflows/ci.yml
git diff --cached --check
git commit -m "ci(backend-v21): gate v21 runtime regressions"
```

## 任务 3：运行 v2.1 目标验证并更新进度文档

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：运行后端 v2.1 回归组**

运行：

```bash
timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21BackgroundJobFoundationTest,BackendV21CalendarRuntimeBridgeTest,BackendV21CollaborationWriteRuntimeTest,BackendV21CommunityRuntimeClosureTest,BackendV21DocsSheetsRuntimeBridgeTest,BackendV21DriveRuntimeBridgeTest,BackendV21EventOutboxFoundationTest,BackendV21MailRuntimeBridgeTest,BackendV21OpsRuntimeBridgeTest,BackendV21PassRuntimeBridgeTest,BackendV21RuntimeContractGapClosureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

预期：PASS，Maven 输出包含：

```text
BUILD SUCCESS
Failures: 0, Errors: 0
```

- [ ] **步骤 2：运行前端完整 contract 测试**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
```

预期：PASS，输出包含：

```text
# fail 0
```

- [ ] **步骤 3：读取上一提交用于进度文档**

运行：

```bash
git log --oneline -2
```

预期：第一行包含：

```text
ci(backend-v21): gate v21 runtime regressions
```

记录这一行的短 SHA 和 subject，用于下一步文档。

- [ ] **步骤 4：更新 completed slice 表格**

在 `docs/superpowers/progress/v21-implementation-progress.md` 的 `Completed v2.1 Slices` 表格末尾追加一行：

```markdown
| Backend v2.1 release gate hardening (`backend-v21-release-gate-hardening`) | `v21-release-gate-hardening-contract.test.mjs`, `scripts/validate-local.sh`, GitHub Actions backend v2.1 runtime regression |
```

- [ ] **步骤 5：更新 Latest Completed Backend Slice**

将 `Latest Completed Backend Slice` 改为：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-release-gate-hardening`
- Commits:
  - `docs(backend-v21): add release gate hardening design`
  - `test(backend-v21): cover release gate runtime regressions`
  - `ci(backend-v21): gate v21 runtime regressions`
- Files changed: added a release gate contract test, promoted all committed `BackendV21*` regressions into `scripts/validate-local.sh`, mirrored the same v2.1 runtime group in GitHub Actions, and kept failures visible through direct Maven exits.
- Verification:
  - `timeout 60s pnpm --dir frontend-v2 exec node --test tests/v21-release-gate-hardening-contract.test.mjs`: PASS
  - `bash -n scripts/validate-local.sh`: PASS
  - `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21BackgroundJobFoundationTest,BackendV21CalendarRuntimeBridgeTest,BackendV21CollaborationWriteRuntimeTest,BackendV21CommunityRuntimeClosureTest,BackendV21DocsSheetsRuntimeBridgeTest,BackendV21DriveRuntimeBridgeTest,BackendV21EventOutboxFoundationTest,BackendV21MailRuntimeBridgeTest,BackendV21OpsRuntimeBridgeTest,BackendV21PassRuntimeBridgeTest,BackendV21RuntimeContractGapClosureTest -Dsurefire.failIfNoSpecifiedTests=false test`: PASS
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS
```

- [ ] **步骤 6：更新 Active Backend Slice**

将 `Active Backend Slice` 改为：

```markdown
## Active Backend Slice

- Slice: `backend-v21-release-gate-hardening`
- Status: `completed`
- Started: `2026-05-14`
- Completed: `2026-05-14`
- Scope: local and CI release gate coverage for every committed `BackendV21*` runtime, contract, access-gate, outbox, job, and bridge regression
- Verification:
  - `timeout 60s pnpm --dir frontend-v2 exec node --test tests/v21-release-gate-hardening-contract.test.mjs`: PASS
  - `bash -n scripts/validate-local.sh`: PASS
  - `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21BackgroundJobFoundationTest,BackendV21CalendarRuntimeBridgeTest,BackendV21CollaborationWriteRuntimeTest,BackendV21CommunityRuntimeClosureTest,BackendV21DocsSheetsRuntimeBridgeTest,BackendV21DriveRuntimeBridgeTest,BackendV21EventOutboxFoundationTest,BackendV21MailRuntimeBridgeTest,BackendV21OpsRuntimeBridgeTest,BackendV21PassRuntimeBridgeTest,BackendV21RuntimeContractGapClosureTest -Dsurefire.failIfNoSpecifiedTests=false test`: PASS
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS
```

- [ ] **步骤 7：更新最新后端提交引用**

将 `Current Repository State` 中的 `Latest backend implementation commit` 改为任务 3 步骤 3 中 `git log --oneline -2` 打印的第一行。提交前确认这一行包含真实短 SHA 和 `ci(backend-v21): gate v21 runtime regressions` subject。

- [ ] **步骤 8：Commit 进度文档**

```bash
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git commit -m "docs(backend-v21): record release gate hardening progress"
```

## 最终验证

- [ ] **步骤 1：确认工作树**

运行：

```bash
git status --short --branch
```

预期：只剩既有无关未跟踪路径：

```text
?? .superpowers/
?? .tmp/
?? docs/MMMail.zip
?? docs/MMMail/
?? frontend/
```

- [ ] **步骤 2：确认最近提交**

运行：

```bash
git log --oneline -6
```

预期：能看到本计划的 test、ci、progress 提交，以及已存在的 design 提交。

- [ ] **步骤 3：最终回报**

回报必须包含：

- 本 slice 名称：`backend-v21-release-gate-hardening`
- 创建/修改的关键文件
- 已通过的验证命令
- 本地 `main` 最新提交
- 明确说明未提交的无关路径仍未纳入

## 计划自检

- 规格覆盖度：设计规格中的本地门禁、CI 门禁、进度文档、失败可见性、范围约束都映射到任务 1-3。
- 标记扫描：本计划不包含待补充标记或开放式实现要求。
- 类型一致性：`BACKEND_V21_RUNTIME_TESTS`、`Backend v2.1 runtime regression`、日志文件名和测试文件名在各任务中保持一致。
- 范围检查：本计划只处理 release gate hardening，不扩展业务运行时，不修复外部 Docker/NVD/container 环境问题。
