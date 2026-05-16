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
    mail: zhCN.page.mail,
    calendar: zhCN.page.calendar,
    drive: zhCN.page.drive,
    driveSecureShare: zhCN.page.driveSecureShare,
    publicShare: zhCN.page.publicShare,
    workspace: zhCN.page.workspace,
    settings: zhCN.page.settings,
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
    billing: zhCN.page.billing
  }
};

export default local;
