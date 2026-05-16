import { lt } from "@/locales";

export const publicAuthWorkspaceItems = [
  { shortLabel: "M", label: lt("邮件", "郵件", "Mail") },
  { shortLabel: "D", label: lt("云盘", "雲端硬碟", "Drive") },
  { shortLabel: "P", label: lt("Pass", "Pass", "Pass") },
  { shortLabel: "A", label: lt("管理", "管理", "Admin") },
] as const;

export const publicAuthSecurityItems = [
  {
    shortLabel: "01",
    label: lt("真实接口提交", "真實介面提交", "Real API submission"),
    description: lt(
      "登录和创建账户都直接写入后端认证会话。",
      "登入和建立帳戶都直接寫入後端驗證工作階段。",
      "Sign-in and account creation write directly to the backend auth session.",
    ),
  },
  {
    shortLabel: "02",
    label: lt("显式错误反馈", "明確錯誤回饋", "Explicit error feedback"),
    description: lt(
      "校验或接口失败会在表单内显示，不做静默降级。",
      "驗證或介面失敗會在表單內顯示，不做靜默降級。",
      "Validation and API failures appear in the form without silent degradation.",
    ),
  },
  {
    shortLabel: "03",
    label: lt("工作台风格一致", "工作台風格一致", "Workspace visual parity"),
    description: lt(
      "入口页复用浅色 surface、细边框和紧凑信息密度。",
      "入口頁複用淺色 surface、細邊框和緊湊資訊密度。",
      "The entry pages reuse light surfaces, fine borders, and compact density.",
    ),
  },
] as const;
