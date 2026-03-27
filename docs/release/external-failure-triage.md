# Community Edition v1.0 External Failure Triage

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 1. GitHub Actions / CI workflow 失败
1. 打开 `MMMail CI / validate` 的 Step Summary
2. 查看 `artifacts/ci-logs/`
3. 查看 `backend/mmmail-server/target/surefire-reports/`
4. 若是 dependency-check 失败，转到本文档第 3 节
5. 若是容器验证失败，转到本文档第 4 节

## 2. Docker-capable runner 不可用
1. 确认 runner 是否可执行 `docker version`
2. 确认 runner 是否允许 `docker compose`
3. 若不可用，标记 Gate 4 / Gate 6 为未解除阻塞
4. 在 `docs/release/community-v1-external-receipt-log.md` 记录失败原因

## 3. dependency-check 报告未产出
1. 检查 `MMMAIL_NVD_API_KEY` 是否已配置
2. 检查 `.tools/dependency-check-data` cache 是否命中
3. 查看 `artifacts/ci-logs/` 中 dependency-check 相关日志
4. 若仍失败，Gate 5 保持 `PASS_CANDIDATE`

## 4. `validate-rc1-container.sh` 失败
1. 先看 `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
2. 再看 `artifacts/release/rc1-container/compose.log`
3. 再看 `artifacts/release/rc1-container/db-*.log`
4. 若前端镜像失败在 `RUN pnpm prepare && pnpm build`，优先检查日志里是否出现 `TS5083` 或 `./.nuxt/tsconfig.json`；这表示 Nuxt 构建目录与根 `tsconfig.json` 不一致，应确认仓库已包含冻结期修复后的 `frontend/scripts/nuxt-command.mjs`
5. 若 `mmmail-redis` 为 `unhealthy`，先执行 `docker compose --env-file artifacts/release/rc1-container/rc1-container.env -f docker-compose.yml logs redis`；确认 Redis 健康检查是否使用了容器内 `SPRING_REDIS_PASSWORD`，仓库应包含修复后的 `docker-compose.yml`
6. 若是服务未起，先排查 Compose / Docker daemon
7. 若是 restore / rollback 失败，保留 Gate 4 为 `IN_PROGRESS`

## 5. artifact 未归档
1. 检查 GitHub Actions artifact 列表是否存在 `mmmail-validate-artifacts`
2. 检查路径是否包含：
   - `artifacts/security/dependency-check/`
   - `artifacts/release/rc1-container/`
3. 若缺失，重新运行对应 job
4. 未归档前不得回填 PASS

## 6. gate 回填信息不完整
1. 打开 `docs/release/gate-backfill-template.md`
2. 补齐执行日期、执行人、workflow run、artifact 路径
3. 更新 `docs/release/community-v1-external-receipt-log.md`
4. 再更新 `docs/release/community-v1-gate.md`
