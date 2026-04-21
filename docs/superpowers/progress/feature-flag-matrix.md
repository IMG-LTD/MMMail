# Feature Flag Matrix

| Frontend Flag | Backend Flag | First Milestone | Rule |
|---|---|---|---|
| `frontendV2.enabled` | `backendV2Identity` + domain flag | M1 | entry may switch only after identity and target domain are live |
| `frontendV2.sharePass.enabled` | `backendV2PublicShare` | M3 | `share/pass` requires both toggles enabled |
| `frontendV2.aiCopilot.enabled` | `backendV2AiPlatform` | M4 | preview/approve/audit path must exist first |
| `frontendV2.automation.enabled` | `backendV2AiPlatform` + `backendV2McpPlatform` | M4 | automation requires both AI and MCP readiness |
| `frontendV2.mcpGovernance.enabled` | `backendV2McpPlatform` | M4 | registry, grant matrix, and audit must all exist |
| `frontendV2.redirects.enabled` | gateway compatibility matrix | M2 | legacy redirect rollout is environment-scoped |
| `frontendV2.labsRedirect.enabled` | `/api/v2/labs/*` reachability | M6 -> M7 | preview-only redirect, never GA |
