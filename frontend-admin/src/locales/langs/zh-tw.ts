import zhCN from './zh-cn';

const local: App.I18n.Schema = {
  ...zhCN,
  system: {
    ...zhCN.system,
    title: 'MMMail'
  },
  route: {
    ...zhCN.route,
    login: '登入'
  },
  page: {
    ...zhCN.page,
    login: {
      ...zhCN.page.login,
      common: {
        ...zhCN.page.login.common,
        loginOrRegister: '登入 / 註冊',
        userNamePlaceholder: '請輸入使用者名稱',
        emailPlaceholder: '請輸入電子郵件',
        passwordPlaceholder: '請輸入密碼',
        confirmPasswordPlaceholder: '請再次輸入密碼',
        loginSuccess: '登入成功',
        welcomeBack: '歡迎回來，{userName}！'
      },
      pwdLogin: {
        ...zhCN.page.login.pwdLogin,
        title: '密碼登入',
        rememberMe: '記住我',
        forgetPassword: '忘記密碼？',
        register: '建立帳戶'
      },
      codeLogin: {
        ...zhCN.page.login.codeLogin,
        unavailable: '驗證碼登入暫未接入後端服務，請使用電子郵件密碼登入'
      },
      register: {
        ...zhCN.page.login.register,
        title: '建立帳戶'
      },
      resetPwd: {
        ...zhCN.page.login.resetPwd,
        title: '重設密碼',
        unavailable: '重設密碼暫未接入後端服務，請聯絡管理員處理'
      }
    },
    accessGate: {
      ...zhCN.page.accessGate,
      backendEntitlement: {
        requiredEdition: '所需版本',
        currentEdition: '目前版本',
        upgradeAction: '升級操作'
      }
    },
    mail: zhCN.page.mail,
    calendar: zhCN.page.calendar,
    drive: zhCN.page.drive,
    driveSecureShare: zhCN.page.driveSecureShare,
    publicShare: zhCN.page.publicShare,
    workspace: zhCN.page.workspace,
    settings: zhCN.page.settings,
    license: {
      ...zhCN.page.license,
      title: '授權憑證',
      state: '授權狀態',
      noEdition: '未授權版本',
      features: '已啟用能力',
      externalBillingStatus: '外部計費狀態',
      expiresAt: '到期時間',
      syncedAt: '同步時間',
      licenseKey: '授權密鑰',
      licenseKeyPlaceholder: '貼上授權密鑰',
      licenseKeyRequired: '請輸入授權密鑰',
      upload: '上傳授權',
      uploadAccepted: '後端已驗證並儲存授權',
      refresh: '重新整理狀態'
    },
    oidc: {
      ...zhCN.page.oidc,
      title: 'OIDC 單一登入',
      configure: '設定 OIDC',
      unavailableTitle: 'OIDC 設定服務尚未接入',
      unavailableDescription: '目前入口只暴露 Business 權益邊界，真實設定介面會在 BUS-01 後端完成後啟用。'
    },
    notifications: zhCN.page.notifications,
    domains: zhCN.page.domains,
    webPush: zhCN.page.webPush,
    docs: zhCN.page.docs,
    sheets: zhCN.page.sheets,
    pass: zhCN.page.pass,
    collaboration: zhCN.page.collaboration,
    commandCenter: zhCN.page.commandCenter,
    admin: zhCN.page.admin,
    contacts: zhCN.page.contacts,
    wallet: zhCN.page.wallet,
    vpn: zhCN.page.vpn,
    meet: zhCN.page.meet,
    authenticator: zhCN.page.authenticator,
    community: zhCN.page.community,
    simpleLogin: zhCN.page.simpleLogin,
    standardNotes: zhCN.page.standardNotes,
    mailFilters: zhCN.page.mailFilters,
    driveVersions: zhCN.page.driveVersions,
    billing: {
      ...zhCN.page.billing,
      externalBillingStatus: '外部計費狀態'
    }
  }
};

export default local;
