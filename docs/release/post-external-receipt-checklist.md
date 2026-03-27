# Post External Receipt Checklist

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 机械执行步骤
1. 收到 workflow run、artifact、report 后，先核对是否包含：
   - `dependency-check-report.html`
   - `dependency-check-report.json`
   - `community-v1-rc1-container-evidence.md`
2. 更新 `docs/release/community-v1-external-receipt-log.md`
3. 按 `docs/release/gate-backfill-template.md` 更新：
   - Gate 4
   - Gate 5
   - Gate 6
4. 更新 `docs/release/community-v1-rc-checklist.md`
5. 更新 `docs/release/community-v1-pre-release-checklist.md`
6. 更新 `docs/release/community-v1-rc-status.md`
   - 将状态从 `RC1_READY_PENDING_EXTERNAL` 切换为 `RC1_READY`
7. 勾选 `docs/release/community-v1-final-signoff.md`
8. 确认当前可进入发布候选确认
