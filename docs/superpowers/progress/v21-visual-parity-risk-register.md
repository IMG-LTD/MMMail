# v2.1 Visual Parity Risk Register

Last updated: 2026-05-12

| UI group | Source design | QA evidence | Status | Notes | Owner slice |
| --- | --- | --- | --- | --- | --- |
| PublicAuthShareSystem | `docs/MMMail/UI/首页/工作台-设计概览.png` and public boundary routes | `login`, `register`, `boundary`, `product-access-blocked` | acceptable-delta | Public pages use MMMail blank-layout branding instead of the historical design sample shell. | frontend-v21-final-visual-parity-public-auth-closure |
| Login | Public auth route and MMMail branding rules | `.tmp/v21-browser-visual-qa/login-desktop.png` | aligned | Login keeps brand story, form, SSO, MFA, and support links visible without claiming auth success. | frontend-v21-final-visual-parity-public-auth-closure |
| Register | Public auth route and MMMail branding rules | `.tmp/v21-browser-visual-qa/register-desktop.png` | aligned | Register remains a public-shell card with explicit account creation boundary. | frontend-v21-final-visual-parity-public-auth-closure |
| Boundary | `docs/MMMail/UI/Admin/管理后台.png` and boundary matrix rules | `.tmp/v21-browser-visual-qa/boundary-desktop.png` | aligned | Boundary page exposes Premium, Hosted, maturity, and permission language. | frontend-v21-final-visual-parity-public-auth-closure |
| System | System state routes | `offline`, `maintenance`, `not-found`, `server-error` | aligned | System pages preserve clear failure and offline states under blank layout. | frontend-v21-final-visual-parity-public-auth-closure |
| Public shares | Public share routes | `share-mail`, `share-drive`, `share-pass` | acceptable-delta | Public share pages are route-specific rather than one generic share design, but all expose concrete shared content states. | frontend-v21-final-visual-parity-public-auth-closure |
