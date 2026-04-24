# MMMail v2 Feedback Intake

**版本**: `v2-feedback-intake`
**日期**: `2026-04-23`

## 当前反馈基线
- 默认公开基线：`main` / `v2.0.4`
- 支持边界：`docs/release/v2-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 如何分流
### Release-blocking regression
适用于：
- `GA` 面的功能回退
- 数据迁移、备份恢复、公开分享、组织隔离、安全能力回退
- `bash scripts/validate-local.sh` 或 CI 默认门禁失败

请附带：
- 复现步骤
- 受影响路由或接口
- 相关日志或报错截图
- 失败命令输出

### Boundary clarification
适用于：
- 不确定某个能力是否属于当前 `GA / Beta / Preview`
- 不确定 Community 是否承诺某个模块、流程或运行模型

请附带：
- 目标模块或路由
- 你预期的支持级别
- 对应文档链接

### Self-hosting question
适用于：
- Compose、环境变量、数据库升级、备份恢复、运行排障

请附带：
- 使用的部署模式（最小 / 标准）
- 关键环境变量是否已替换占位值
- 相关脚本输出或容器日志

### Preview request
适用于：
- `Labs`、聚合面或未承诺能力的方向建议

请附带：
- 目标场景
- 业务价值
- 为什么当前 `GA / Beta` 能力不足

## 证据建议
- `bash scripts/validate-local.sh`
- `bash scripts/validate-security.sh`
- `./scripts/db-upgrade.sh .env info`
- `docker compose --env-file .env -f docker-compose.minimal.yml ps`
- `docker compose logs backend`

## 不要在公开 issue 中提交
- 活跃安全漏洞细节
- 真实 secrets、令牌、密码
- 生产数据库快照或私有基础设施地址

安全问题请改走 `SECURITY.md`。
