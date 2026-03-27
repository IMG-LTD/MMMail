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
