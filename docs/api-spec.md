# API 规范（Phase 8）

**版本**: v88.0  
**日期**: 2026-03-09

## 1. Docs suggestions API
### `GET /api/v1/docs/notes/{noteId}/suggestions`
查询参数：
- `includeResolved`：`true | false`

响应示例：
```json
[
  {
    "suggestionId": "2030928911626999809",
    "noteId": "2030927924510134274",
    "authorUserId": "2030927607554965505",
    "authorEmail": "editor@mmmail.local",
    "authorDisplayName": "Editor",
    "baseVersion": 2,
    "selectionStart": 34,
    "selectionEnd": 89,
    "originalText": "This sentence will be replaced by an editor suggestion.",
    "replacementText": "This sentence has been updated through the suggestion workflow.",
    "status": "PENDING",
    "createdAt": "2026-03-09T16:49:20",
    "resolvedAt": null,
    "resolvedByUserId": null,
    "resolvedByEmail": null,
    "resolvedByDisplayName": null
  }
]
```

### `POST /api/v1/docs/notes/{noteId}/suggestions`
请求体：
```json
{
  "baseVersion": 2,
  "selectionStart": 34,
  "selectionEnd": 89,
  "originalText": "This sentence will be replaced by an editor suggestion.",
  "replacementText": "This sentence has been updated through the suggestion workflow."
}
```

规则：
- 仅 `OWNER / EDIT` 权限用户可创建 suggestion
- `selectionStart < selectionEnd`
- `originalText` 必须与当前 note 内容对应区间严格匹配

### `POST /api/v1/docs/notes/{noteId}/suggestions/{suggestionId}/accept`
请求体：
```json
{
  "currentVersion": 2
}
```

行为：
- 仅 owner 可接受 suggestion
- 接受成功后 note 正文更新且版本递增
- 若版本或原文不匹配，返回 `409 CONFLICT`

### `POST /api/v1/docs/notes/{noteId}/suggestions/{suggestionId}/reject`
行为：
- 仅 owner 可拒绝 suggestion
- 正文保持不变，suggestion 状态转为 `REJECTED`

## 2. Docs share permission update
### `PUT /api/v1/docs/notes/{noteId}/shares/{shareId}`
请求体：
```json
{
  "permission": "EDIT"
}
```

规则：
- 仅 owner 可更新 share 权限
- 允许值：`VIEW`、`EDIT`
- 更新结果同时进入 Docs sync / suite collaboration 事件流

## 3. 错误码
- `30040 DOCS_NOTE_SUGGESTION_NOT_FOUND`
- `30041 DOCS_NOTE_SUGGESTION_CONFLICT`

其中：
- `30041` -> HTTP `409 CONFLICT`
- 不做 silent fallback；冲突条件必须显式返回给前端

## 4. v2.1.2 错误码与 i18n 键

所有前端可见错误文案必须通过 `errors.{code}.title` 与 `errors.{code}.message` 读取，后端只返回稳定 `code` 与 `message`。

| Code | ErrorCode | HTTP 语义 | i18n keys |
|---|---|---|---|
| `30052` | `V2_ENTITLEMENT_REQUIRED` | `403 FORBIDDEN` | `errors.30052.title`, `errors.30052.message` |
| `30053` | `V2_PERMISSION_DENIED` | `403 FORBIDDEN` | `errors.30053.title`, `errors.30053.message` |
| `30055` | `CALENDAR_SUBSCRIPTION_NOT_FOUND` | `404 NOT_FOUND` | `errors.30055.title`, `errors.30055.message` |
| `30056` | `MAIL_EXTERNAL_ACCOUNT_NOT_FOUND` | `404 NOT_FOUND` | `errors.30056.title`, `errors.30056.message` |
| `40021` | `COMMUNITY_TITLE_REQUIRED` | `400 BAD_REQUEST` | `errors.40021.title`, `errors.40021.message` |
| `40022` | `COMMUNITY_TOPIC_NOT_FOUND` | `404 NOT_FOUND` | `errors.40022.title`, `errors.40022.message` |
| `40023` | `COMMUNITY_POST_NOT_FOUND` | `404 NOT_FOUND` | `errors.40023.title`, `errors.40023.message` |
| `40024` | `COMMUNITY_COMMENT_NOT_FOUND` | `404 NOT_FOUND` | `errors.40024.title`, `errors.40024.message` |
| `40025` | `COMMUNITY_REPORT_NOT_FOUND` | `404 NOT_FOUND` | `errors.40025.title`, `errors.40025.message` |
| `40026` | `SEARCH_QUERY_TOO_SHORT` | `400 BAD_REQUEST` | `errors.40026.title`, `errors.40026.message` |
| `40027` | `SEARCH_REINDEX_JOB_NOT_FOUND` | `404 NOT_FOUND` | `errors.40027.title`, `errors.40027.message` |
| `40028` | `SEARCH_MODULE_UNSUPPORTED` | `400 BAD_REQUEST` | `errors.40028.title`, `errors.40028.message` |
| `40029` | `MAIL_EXTERNAL_ACCOUNT_CONFIG` | `400 BAD_REQUEST` | `errors.40029.title`, `errors.40029.message` |
| `40121` | `MAIL_EXTERNAL_AUTH_INVALID` | `401 UNAUTHORIZED` | `errors.40121.title`, `errors.40121.message` |
| `40321` | `COMMUNITY_NOT_AUTHOR` | `403 FORBIDDEN` | `errors.40321.title`, `errors.40321.message` |
| `40322` | `COMMUNITY_ADMIN_REQUIRED` | `403 FORBIDDEN` | `errors.40322.title`, `errors.40322.message` |
| `40921` | `COMMUNITY_POST_LOCKED` | `409 CONFLICT` | `errors.40921.title`, `errors.40921.message` |
| `40922` | `COMMUNITY_TOPIC_NOT_EMPTY` | `409 CONFLICT` | `errors.40922.title`, `errors.40922.message` |
| `42221` | `SHEETS_CIRCULAR_REF` | `422 UNPROCESSABLE_ENTITY` | `errors.42221.title`, `errors.42221.message` |
| `50421` | `MAIL_EXTERNAL_TIMEOUT` | `504 GATEWAY_TIMEOUT` | `errors.50421.title`, `errors.50421.message` |
| `50521` | `MAIL_EXTERNAL_RATE_LIMITED` | `429 TOO_MANY_REQUESTS` | `errors.50521.title`, `errors.50521.message` |

## 5. v2.1.2 新增控制器与端点

本节同步 `docs/v212-migration-spec.md §26.6` 要求的新增后端控制器 API 文档。所有端点继续使用统一 `Result<T>` 响应 envelope，认证失败返回稳定错误码，不做 silent fallback。

### CommunityController

Base path: `/api/v1/community`

- `GET /api/v1/community/topics`：列出话题。
- `POST /api/v1/community/topics`：管理员创建话题。
- `PATCH /api/v1/community/topics/{topicId}`：管理员更新话题。
- `DELETE /api/v1/community/topics/{topicId}`：删除空话题，非空返回 `COMMUNITY_TOPIC_NOT_EMPTY`。
- `GET /api/v1/community/tags?limit=20`：列出热门标签。
- `GET /api/v1/community/posts`：分页查询 posts，支持 `topicId`、`q`、`page`、`size`、`sort`。
- `POST /api/v1/community/posts`：创建帖子。
- `GET /api/v1/community/posts/{postId}`：读取帖子详情。
- `PATCH /api/v1/community/posts/{postId}`：作者更新帖子。
- `DELETE /api/v1/community/posts/{postId}`：作者软删帖子。
- `POST /api/v1/community/posts/{postId}/pin`：管理员置顶。
- `POST /api/v1/community/posts/{postId}/lock`：管理员锁定。
- `GET /api/v1/community/posts/{postId}/comments`：列出评论。
- `POST /api/v1/community/posts/{postId}/comments`：创建评论。
- `DELETE /api/v1/community/comments/{commentId}`：删除评论并保留占位。
- `POST /api/v1/community/posts/{postId}/like`：切换点赞。
- `POST /api/v1/community/posts/{postId}/bookmark`：切换收藏。
- `POST /api/v1/community/posts/{postId}/view`：记录浏览。
- `GET /api/v1/community/me/bookmarks`：查询我的收藏。
- `POST /api/v1/community/reports`：创建举报。
- `GET /api/v1/community/admin/reports`：管理员 moderation 队列。
- `PATCH /api/v1/community/admin/reports/{reportId}`：管理员 moderation 处理。

### SearchController

Base path: `/api/v1/search`

- `GET /api/v1/search`：全局搜索，查询参数通过 `SearchQueryParams` 绑定。
- `GET /api/v1/search/suggestions?q={query}`：搜索建议。
- `GET /api/v1/search/facets`：按当前查询条件返回聚合 facet。
- `POST /api/v1/search/reindex/{moduleType}`：管理员发起模块重建任务。
- `GET /api/v1/search/reindex/{jobId}`：管理员读取重建任务状态。

### DomainController

Base path: `/api/v1/domains`

- 所有请求必须带 `X-Org-Id`。
- `GET /api/v1/domains`：列出组织域名。
- `POST /api/v1/domains`：创建域名。
- `GET /api/v1/domains/{domainId}`：读取域名详情。
- `DELETE /api/v1/domains/{domainId}`：删除域名。
- `POST /api/v1/domains/{domainId}/verify`：触发 DNS 校验。
- `GET /api/v1/domains/{domainId}/dns-records`：返回期望 dns-records。
- `GET /api/v1/domains/{domainId}/diagnostics`：返回 DNS diagnostics。

### WebPushController

Base path: `/api/v1/web-push`

- `GET /api/v1/web-push/vapid-public-key`：读取 VAPID 公钥。
- `GET /api/v1/web-push/subscriptions`：列出当前用户 subscriptions。
- `POST /api/v1/web-push/subscriptions`：注册订阅。
- `DELETE /api/v1/web-push/subscriptions/{subscriptionId}`：删除订阅。
- `POST /api/v1/web-push/test`：触发测试推送，受 v2.1.2 敏感写操作限流保护。

### SecurityEventController

User surface:

- `GET /api/v1/security/events`：列出当前用户安全事件，支持 `type` 与 `page`。
- `POST /api/v1/security/events/{id}/ack`：用户确认事件。

Admin surface:

- `GET /api/v1/admin/security/anomalies`：管理员查询异常登录事件。
- `POST /api/v1/admin/security/anomalies/{id}/action`：管理员执行安全事件 action。

### SettingsController feature flags

- `GET /api/v1/settings/feature-flags`：返回当前 `feature_flag` 表中启用的 flag key 列表。
- 登录与注册响应中的 `featureFlags` 使用同一来源，不再硬编码单一 flag。

### MailExternalAccountController

Base path: `/api/v1/mail/external-accounts`

- `GET /api/v1/mail/external-accounts`：列出外部账号。
- `POST /api/v1/mail/external-accounts`：创建外部账号，返回 `201`。
- `GET /api/v1/mail/external-accounts/{accountId}`：读取账号详情。
- `PATCH /api/v1/mail/external-accounts/{accountId}`：更新账号配置。
- `DELETE /api/v1/mail/external-accounts/{accountId}`：删除账号。
- `POST /api/v1/mail/external-accounts/{accountId}/test`：测试 IMAP / SMTP 配置。
- `POST /api/v1/mail/external-accounts/{accountId}/sync`：触发同步。

### SheetsFormulaController

Base path: `/api/v1/sheets/{workbookId}`

- `POST /api/v1/sheets/{workbookId}/cells/evaluate`：评估一组单元格公式。
- `GET /api/v1/sheets/{workbookId}/dependency-graph`：读取公式依赖图。
- `POST /api/v1/sheets/{workbookId}/recalculate`：重新计算工作簿。

### CollabController

Base path: `/api/v1/collab/{resourceType}/{resourceId}`

- `GET /api/v1/collab/{resourceType}/{resourceId}/snapshot`：读取 CRDT snapshot。
- `POST /api/v1/collab/{resourceType}/{resourceId}/snapshot`：写入 CRDT snapshot。
- `GET /api/v1/collab/{resourceType}/{resourceId}/awareness`：读取协同 awareness。
- WebSocket updates 通过协同 WebSocket 通道传输，不通过该 HTTP controller 暴露伪 `/updates` 成功路径。

### NotificationRealtimeController

Base path: `/api/v2/notifications`

- `GET /api/v2/notifications/since?cursor={cursor}&limit={limit}`：按 cursor 补发通知事件。
- WebSocket 通知流使用同一 cursor 语义；断线后客户端用 `since` 回放缺失事件。

### V21OpsController command panel

Base path: `/api/v2`

- `GET /api/v2/command-center/commands`：列出命令。
- `GET /api/v2/command-center/commands/{id}`：读取命令详情。
- `GET /api/v2/command-center/catalog`：读取可用 catalog。
- `GET /api/v2/command-center/recents`：读取 recents。
- `POST /api/v2/command-center/pin`：固定或取消固定命令。
- `GET /api/v2/command-center/quick-search`：命令快速搜索。
- `POST /api/v2/command-center/runs`、`/runs/{id}/cancel`、`/runs/{id}/retry`：当前为显式 premium unsupported，仍接入命令运行限流。
