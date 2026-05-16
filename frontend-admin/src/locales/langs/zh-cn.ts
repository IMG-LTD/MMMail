import { zhCNErrorMessages } from './v212-error-messages';
import { v22CommercialPageLocale } from './v22-commercial/zh-cn';

const local: App.I18n.Schema = {
  system: {
    title: 'MMMail',
    updateTitle: '系统版本更新通知',
    updateContent: '检测到系统有新版本发布，是否立即刷新页面？',
    updateConfirm: '立即刷新',
    updateCancel: '稍后再说'
  },
  common: {
    action: '操作',
    add: '新增',
    addSuccess: '添加成功',
    backToHome: '返回首页',
    batchDelete: '批量删除',
    cancel: '取消',
    close: '关闭',
    check: '勾选',
    select: '选择',
    selectAll: '全选',
    expandColumn: '展开列',
    columnSetting: '列设置',
    config: '配置',
    confirm: '确认',
    delete: '删除',
    deleteSuccess: '删除成功',
    confirmDelete: '确认删除吗？',
    edit: '编辑',
    warning: '警告',
    error: '错误',
    index: '序号',
    keywordSearch: '请输入关键词搜索',
    logout: '退出登录',
    logoutConfirm: '确认退出登录吗？',
    lookForward: '敬请期待',
    modify: '修改',
    modifySuccess: '修改成功',
    noData: '无数据',
    operate: '操作',
    pleaseCheckValue: '请检查输入的值是否合法',
    refresh: '刷新',
    reset: '重置',
    search: '搜索',
    switch: '切换',
    tip: '提示',
    trigger: '触发',
    update: '更新',
    updateSuccess: '更新成功',
    userCenter: '个人中心',
    yesOrNo: {
      yes: '是',
      no: '否'
    }
  },
  request: {
    logout: '请求失败后登出用户',
    logoutMsg: '用户状态失效，请重新登录',
    logoutWithModal: '请求失败后弹出模态框再登出用户',
    logoutWithModalMsg: '用户状态失效，请重新登录',
    refreshToken: '请求的token已过期，刷新token',
    tokenExpired: 'token已过期',
    traceId: '追踪 ID',
    copyDetails: '复制详情',
    copyDetailsSuccess: '错误详情已复制',
    copyDetailsFailed: '错误详情复制失败'
  },
  errors: zhCNErrorMessages,
  theme: {
    themeDrawerTitle: '主题配置',
    tabs: {
      appearance: '外观',
      layout: '布局',
      general: '通用',
      preset: '预设'
    },
    appearance: {
      themeSchema: {
        title: '主题模式',
        light: '亮色模式',
        dark: '暗黑模式',
        auto: '跟随系统'
      },
      grayscale: '灰色模式',
      colourWeakness: '色弱模式',
      themeColor: {
        title: '主题颜色',
        primary: '主色',
        info: '信息色',
        success: '成功色',
        warning: '警告色',
        error: '错误色',
        followPrimary: '跟随主色'
      },
      themeRadius: {
        title: '主题圆角'
      },
      recommendColor: '应用推荐算法的颜色',
      recommendColorDesc: '推荐颜色的算法参照',
      preset: {
        title: '主题预设',
        apply: '应用',
        applySuccess: '预设应用成功',
        default: {
          name: '默认预设',
          desc: '适用于 MMMail 工作台的默认主题预设'
        },
        dark: {
          name: '暗色预设',
          desc: '适用于夜间使用的暗色主题预设'
        },
        compact: {
          name: '紧凑型',
          desc: '适用于小屏幕的紧凑布局预设'
        },
        azir: {
          name: 'Azir的预设',
          desc: '是 Azir 比较喜欢的莫兰迪色系冷淡风'
        }
      }
    },
    layout: {
      layoutMode: {
        title: '布局模式',
        vertical: '左侧菜单模式',
        'vertical-mix': '左侧菜单混合模式',
        'vertical-hybrid-header-first': '左侧混合-顶部优先',
        horizontal: '顶部菜单模式',
        'top-hybrid-sidebar-first': '顶部混合-侧边优先',
        'top-hybrid-header-first': '顶部混合-顶部优先',
        vertical_detail: '左侧菜单布局，菜单在左，内容在右。',
        'vertical-mix_detail': '左侧双菜单布局，一级菜单在左侧深色区域，二级菜单在左侧浅色区域。',
        'vertical-hybrid-header-first_detail':
          '左侧混合布局，一级菜单在顶部，二级菜单在左侧深色区域，三级菜单在左侧浅色区域。',
        horizontal_detail: '顶部菜单布局，菜单在顶部，内容在下方。',
        'top-hybrid-sidebar-first_detail': '顶部混合布局，一级菜单在左侧，二级菜单在顶部。',
        'top-hybrid-header-first_detail': '顶部混合布局，一级菜单在顶部，二级菜单在左侧。'
      },
      tab: {
        title: '标签栏设置',
        visible: '显示标签栏',
        cache: '标签栏信息缓存',
        cacheTip: '离开页面后仍然保留标签栏信息',
        height: '标签栏高度',
        mode: {
          title: '标签栏风格',
          slider: '滑块风格',
          chrome: '谷歌风格',
          button: '按钮风格'
        },
        closeByMiddleClick: '鼠标中键关闭标签页',
        closeByMiddleClickTip: '启用后可以使用鼠标中键点击标签页进行关闭'
      },
      header: {
        title: '头部设置',
        height: '头部高度',
        breadcrumb: {
          visible: '显示面包屑',
          showIcon: '显示面包屑图标'
        }
      },
      sider: {
        title: '侧边栏设置',
        inverted: '深色侧边栏',
        width: '侧边栏宽度',
        collapsedWidth: '侧边栏折叠宽度',
        mixWidth: '混合布局侧边栏宽度',
        mixCollapsedWidth: '混合布局侧边栏折叠宽度',
        mixChildMenuWidth: '混合布局子菜单宽度',
        autoSelectFirstMenu: '自动选择第一个子菜单',
        autoSelectFirstMenuTip: '点击一级菜单时，自动选择并导航到第一个子菜单的最深层级'
      },
      footer: {
        title: '底部设置',
        visible: '显示底部',
        fixed: '固定底部',
        height: '底部高度',
        right: '底部居右'
      },
      content: {
        title: '内容区域设置',
        scrollMode: {
          title: '滚动模式',
          tip: '主题滚动仅 main 部分滚动，外层滚动可携带头部底部一起滚动',
          wrapper: '外层滚动',
          content: '主体滚动'
        },
        page: {
          animate: '页面切换动画',
          mode: {
            title: '页面切换动画类型',
            'fade-slide': '滑动',
            fade: '淡入淡出',
            'fade-bottom': '底部消退',
            'fade-scale': '缩放消退',
            'zoom-fade': '渐变',
            'zoom-out': '闪现',
            none: '无'
          }
        },
        fixedHeaderAndTab: '固定头部和标签栏'
      }
    },
    general: {
      title: '通用设置',
      watermark: {
        title: '水印设置',
        visible: '显示全屏水印',
        text: '自定义水印文本',
        enableUserName: '启用用户名水印',
        enableTime: '显示当前时间',
        timeFormat: '时间格式'
      },
      multilingual: {
        title: '多语言设置',
        visible: '显示多语言按钮'
      },
      globalSearch: {
        title: '全局搜索设置',
        visible: '显示全局搜索按钮'
      }
    },
    configOperation: {
      copyConfig: '复制配置',
      copySuccessMsg: '复制成功，请替换 src/theme/settings.ts 中的变量 themeSettings',
      resetConfig: '重置配置',
      resetSuccessMsg: '重置成功'
    }
  },
  route: {
    login: '登录',
    403: '无权限',
    404: '页面不存在',
    500: '服务器错误',
    'iframe-page': '外链页面',
    home: '首页',
    'business-overview': '组织总览',
    mail: '邮件',
    calendar: '日历',
    drive: '网盘',
    docs: '文档',
    sheets: '表格',
    pass: '密码库',
    collaboration: '协作',
    'command-center': '指挥中心',
    notifications: '通知',
    admin: '管理',
    settings: '设置',
    share: '公开分享',
    community: '社区',
    contacts: '联系人',
    wallet: '钱包',
    vpn: 'VPN',
    meet: '会议',
    integrations: '集成',
    integrations_simplelogin: 'SimpleLogin',
    notes: '笔记',
    security: '安全',
    search: '搜索',
    security_authenticator: '安全验证器'
  },
  page: {
    state: {
      loading: { description: '加载中…' },
      empty: { description: '暂无数据' },
      error: { title: '加载失败', description: '请稍后重试，或联系管理员。' }
    },
    businessOverview: {
      statsTitle: '组织概览',
      teamSpacesTitle: '团队空间',
      memberCount: '成员数',
      adminCount: '管理员',
      pendingInviteCount: '待邀请',
      teamSpaceCount: '团队空间数',
      storage: '存储用量',
      governanceSla: '审计 SLA',
      dualReview: '双人复核',
      openSpace: '查看',
      download: '下载',
      column: {
        name: '名称',
        role: '角色',
        itemCount: '条目数',
        storage: '存储',
        updatedAt: '更新时间'
      }
    },
    accessGate: {
      upgrade: {
        title: '升级套餐解锁此功能',
        description: '当前组织未包含所需权益，请升级后继续使用。',
        primary: '立即升级',
        secondary: '了解更多'
      },
      'contact-sales': {
        title: '此功能需要联系销售',
        description: '该能力适用于更高阶组织场景，我们会协助你完成开通。',
        primary: '联系销售',
        secondary: '加入 Waitlist'
      },
      trial: {
        title: '免费试用 14 天',
        description: '开启试用后即可验证完整能力，到期前不会影响当前数据。',
        primary: '启动试用',
        secondary: '查看条款'
      },
      forbidden: {
        title: '无权限',
        description: '当前账号没有访问此页面的权限。',
        primary: '返回首页',
        secondary: '联系管理员'
      },
      backendEntitlement: v22CommercialPageLocale.accessGate.backendEntitlement
    },
    login: {
      common: {
        loginOrRegister: '登录 / 注册',
        userNamePlaceholder: '请输入用户名',
        emailPlaceholder: '请输入邮箱',
        phonePlaceholder: '请输入手机号',
        codePlaceholder: '请输入验证码',
        passwordPlaceholder: '请输入密码',
        confirmPasswordPlaceholder: '请再次输入密码',
        codeLogin: '验证码登录',
        confirm: '确定',
        back: '返回',
        loginSuccess: '登录成功',
        welcomeBack: '欢迎回来，{userName} ！',
        headline: '进入 MMMail 工作台',
        subtitle: '用统一身份访问邮件、云盘、日历和管理能力。'
      },
      pwdLogin: {
        title: '密码登录',
        rememberMe: '记住我',
        forgetPassword: '忘记密码？',
        register: '注册账号',
        otherAccountLogin: '其他账号登录',
        otherLoginMode: '其他登录方式',
        superAdmin: '超级管理员',
        admin: '管理员',
        user: '普通用户'
      },
      codeLogin: {
        title: '验证码登录',
        getCode: '获取验证码',
        reGetCode: '{time}秒后重新获取',
        imageCodePlaceholder: '请输入图片验证码',
        unavailable: '验证码登录暂未接入后端服务，请使用邮箱密码登录'
      },
      register: {
        title: '创建账户',
        submit: '创建账户',
        agreement: '我已经仔细阅读并接受',
        protocol: '《用户协议》',
        policy: '《隐私权政策》'
      },
      security: {
        title: '需要安全验证',
        secondFactorRequired: '检测到新的登录位置，请完成二次验证后继续。',
        lockTitle: '账户暂时锁定',
        lockCountdown: '连续登录失败次数过多，请在 {time} 后重试。'
      },
      resetPwd: {
        title: '重置密码',
        unavailable: '重置密码暂未接入后端服务，请联系管理员处理'
      },
      bindWeChat: {
        title: '绑定微信'
      }
    },
    mail: {
      compose: '写邮件',
      sender: '发件人',
      subject: '主题',
      preview: '摘要',
      time: '时间',
      to: '收件人',
      body: '正文',
      send: '发送',
      saveDraft: '保存草稿',
      bulkMarkRead: '标记已读',
      bulkDelete: '批量删除',
      selectedCount: '已选择 {count} 封',
      reader: '邮件阅读',
      externalAccounts: '外部账号',
      provider: '服务商',
      authMode: '认证方式',
      accountEmail: '账号邮箱',
      username: '用户名',
      password: '授权密码',
      oauthToken: 'OAuth 刷新令牌',
      imapHost: 'IMAP 主机',
      imapPort: 'IMAP 端口',
      smtpHost: 'SMTP 主机',
      smtpPort: 'SMTP 端口',
      addAccount: '保存账号',
      testConnection: '测试连接',
      sync: '同步',
      deleteAccount: '删除账号'
    },
    calendar: {
      create: '新建日程',
      title: '标题',
      location: '地点',
      startAt: '开始时间',
      endAt: '结束时间',
      rrule: '重复规则',
      attendees: '参与者',
      availability: '可用性',
      conflict: '存在时间冲突',
      noConflict: '当前时间可用',
      settings: '日历设置',
      deleteSelected: '删除选中',
      subscriptions: '日历订阅',
      subscriptionLabel: '订阅名称',
      sourceUrl: '订阅地址',
      status: '状态',
      updatedAt: '更新时间',
      color: '颜色',
      sync: '同步',
      exportIcs: '导出 ICS'
    },
    drive: {
      upload: '上传文件',
      storage: '存储用量',
      folders: '文件夹',
      share: '分享',
      permission: '权限',
      deleteSelected: '删除选中',
      fileName: '文件名',
      sizeBytes: '大小',
      updatedAt: '更新时间'
    },
    driveSecureShare: {
      title: '加密分享',
      encryptedCopy: '加密副本',
      sharePassword: '访问密码',
      localKey: '本地密钥片段',
      localKeyWarning: '密钥片段不会发送到服务器，只会附加在分享链接的 #k= 后面。',
      secureLink: '安全链接',
      create: '创建加密分享'
    },
    publicShare: v22CommercialPageLocale.publicShare,
    workspace: {
      systemStatus: '系统状态',
      recommendations: '建议',
      activity: '活动',
      tasks: '任务',
      completed: '已完成'
    },
    settings: {
      profile: '个人资料',
      security: '安全',
      devices: '设备',
      notifications: '通知偏好',
      displayName: '显示名称',
      signature: '签名',
      timezone: '时区',
      preferredLocale: '偏好语言',
      mailAddressMode: '邮箱地址模式',
      autoSaveSeconds: '自动保存秒数',
      undoSendSeconds: '撤销发送秒数',
      recoveryEmail: '恢复邮箱',
      mfaEnabled: '启用多因素认证',
      emailDigest: '邮件摘要',
      productUpdates: '产品更新',
      revokeDevice: '移除设备'
    },
    license: v22CommercialPageLocale.license,
    oidc: v22CommercialPageLocale.oidc,
    notifications: {
      title: '标题',
      body: '内容',
      product: '产品',
      severity: '级别',
      status: '状态',
      createdAt: '创建时间',
      markRead: '标记已读',
      archive: '归档',
      subscriptions: '订阅',
      unread: '未读'
    },
    domains: {
      title: '自定义域名',
      domain: '域名',
      token: '验证令牌',
      verify: '验证域名',
      setDefault: '设为默认',
      diagnostics: 'DNS 诊断',
      type: '类型',
      host: '主机',
      expected: '期望值',
      actual: '当前值',
      matched: '匹配'
    },
    webPush: {
      title: 'Web Push',
      endpoint: '推送端点',
      p256dh: 'P-256 DH 公钥',
      auth: '认证密钥',
      label: '标签',
      userAgent: 'User Agent',
      register: '注册订阅',
      test: '测试推送'
    },
    docs: {
      title: '文档标题',
      content: '内容',
      create: '新建文档',
      save: '保存文档'
    },
    sheets: {
      title: '表格标题',
      rows: '行数',
      columns: '列数',
      create: '新建表格',
      saveCell: '保存单元格',
      formula: '公式',
      evaluate: '计算',
      recalculate: '重算',
      value: '值',
      dependencies: '依赖',
      dependents: '被依赖',
      dependencyGraph: '依赖图'
    },
    pass: {
      title: '条目标题',
      website: '网站',
      username: '用户名',
      secret: '密文',
      create: '新建条目',
      monitor: '风险监控'
    },
    collaboration: {
      title: '协作标题',
      product: '产品',
      status: '状态',
      create: '新建项目',
      tasks: '任务'
    },
    commandCenter: {
      title: '命令',
      description: '描述',
      product: '产品',
      enabled: '启用',
      catalog: '命令目录',
      recents: '最近使用',
      systemCommands: '系统命令',
      search: '快速搜索',
      searchPlaceholder: '搜索命令、邮件、文件或安全项',
      group: '分组',
      shortcut: '快捷键',
      route: '路由',
      pinned: '已固定',
      execute: '执行',
      pin: '固定',
      unpin: '取消固定',
      source: '来源',
      usageCount: '使用次数'
    },
    admin: {
      title: '管理后台',
      domains: '域名',
      productAccess: '产品权限',
      sessions: '成员会话',
      members: '成员'
    },
    contacts: {
      title: '联系人',
      name: '姓名',
      email: '邮箱',
      note: '备注',
      create: '新建联系人',
      groups: '联系人组'
    },
    wallet: {
      title: '钱包',
      account: '账户',
      asset: '资产',
      balance: '余额',
      transaction: '交易',
      type: '类型',
      amount: '金额',
      execution: '执行概览',
      health: '健康分',
      transactions: '交易记录',
      accounts: '账户',
      send: '转账',
      receive: '收款',
      reconciliation: '对账',
      transactionDetail: '交易详情',
      createAccount: '创建账户',
      executionPlan: '执行计划',
      pendingActions: '待处理动作',
      trace: '执行轨迹',
      integrity: '完整性',
      risk: '风险',
      blocked: '阻塞',
      mismatch: '差异',
      sourceAddress: '来源地址',
      targetAddress: '目标地址',
      memo: '备注',
      networkTxHash: '链上 Tx Hash',
      signerHint: '签名人',
      operatorHint: '操作人',
      address: '地址',
      confirmations: '确认数',
      maxItems: '最大条数',
      strategy: '策略',
      reason: '原因',
      action: '动作',
      sign: '签名',
      broadcast: '广播',
      confirm: '确认',
      remediate: '补救',
      fail: '标记失败',
      batchAdvance: '批量推进',
      batchRemediate: '批量补救',
      batchReconcile: '批量对账'
    },
    vpn: {
      title: 'VPN',
      country: '国家',
      city: '城市',
      tier: '等级',
      load: '负载',
      session: '连接会话',
      sessions: '会话记录',
      servers: '服务器',
      profiles: '连接配置',
      settings: '连接设置',
      name: '名称',
      protocol: '协议',
      routingMode: '路由模式',
      targetServer: '目标服务器',
      targetCountry: '目标国家',
      secureCore: '安全核心',
      netshield: '网络防护',
      killSwitch: '断网保护',
      defaultMode: '默认连接',
      connect: '连接',
      history: '历史记录',
      createProfile: '创建配置',
      updateProfile: '更新配置',
      quickConnect: '快速连接',
      disconnect: '断开连接'
    },
    meet: {
      title: '会议',
      topic: '主题',
      accessLevel: '访问级别',
      startedAt: '开始时间',
      plan: '套餐',
      current: '当前会议',
      create: '新建会议',
      maxParticipants: '最大人数',
      access: '接入',
      rooms: '会议室',
      room: '会议室',
      lobby: '入会预览',
      join: '加入会议',
      host: '主持人',
      waitlist: '加入候补',
      activate: '激活',
      contactSales: '联系销售',
      company: '公司',
      seats: '席位',
      note: '备注',
      history: '历史会议',
      participants: '参与者',
      guestRequests: '访客请求',
      signals: '信令',
      quality: '质量',
      displayName: '显示名称',
      joinCode: '加入码',
      requestToken: '请求令牌',
      guestSession: '访客会话',
      audio: '音频',
      video: '视频',
      screen: '屏幕',
      approve: '批准',
      reject: '拒绝',
      rotateJoinCode: '轮换加入码',
      endRoom: '结束会议',
      heartbeat: '心跳',
      payload: '载荷',
      jitter: '抖动',
      packetLoss: '丢包',
      roundTrip: '往返延迟'
    },
    authenticator: {
      title: '安全验证器',
      issuer: '发行方',
      account: '账户',
      algorithm: '算法',
      secret: '密文密钥',
      create: '新增验证器',
      generate: '生成验证码',
      sync: '同步',
      import: '导入',
      backup: '备份',
      settings: '安全设置',
      pin: 'PIN',
      qrImage: 'QR 图片',
      exported: '导出内容',
      lockTimeout: '锁定超时'
    },
    community: {
      title: '社区',
      posts: '帖子',
      newPost: '发布帖子',
      topic: '话题',
      postTitle: '标题',
      tags: '标签',
      body: '正文',
      publish: '发布',
      empty: '暂无社区内容',
      comment: '评论',
      send: '发送',
      report: '举报',
      bookmark: '收藏',
      like: '赞'
    },
    simpleLogin: {
      title: 'SimpleLogin',
      aliases: '别名',
      enabled: '已启用',
      domains: '已验证域名',
      domain: '域名',
      defaultMailbox: '默认邮箱',
      defaultMailboxId: '默认邮箱 ID',
      customDomainId: '自定义域名 ID',
      subdomainMode: '子域模式',
      catchAll: 'Catch-all',
      createPolicy: '新建中继策略'
    },
    standardNotes: {
      title: '笔记',
      folders: '文件夹',
      total: '笔记总数',
      checklist: '清单项',
      exported: '导出条目',
      editor: '编辑器',
      noteTitle: '笔记标题',
      type: '类型',
      folder: '文件夹',
      tags: '标签',
      create: '新建笔记',
      toggleChecklist: '切换首个清单项',
      export: '导出'
    },
    mailFilters: {
      title: '邮件规则',
      name: '规则名称',
      targetFolder: '目标文件夹',
      labels: '标签',
      markRead: '标记已读',
      create: '新建规则',
      preview: '预览规则'
    },
    driveVersions: {
      title: '版本历史',
      version: '版本',
      author: '作者',
      compare: '对比',
      checksum: '校验和',
      createdAt: '创建时间',
      restoreConfirm: '还原后会生成新的当前版本，确认继续？',
      restore: '还原版本',
      e2eeReady: '加密分享能力可用'
    },
    billing: v22CommercialPageLocale.billing
  },
  form: {
    required: '不能为空',
    userName: {
      required: '请输入用户名',
      invalid: '用户名格式不正确'
    },
    phone: {
      required: '请输入手机号',
      invalid: '手机号格式不正确'
    },
    pwd: {
      required: '请输入密码',
      invalid: '密码格式不正确，6-18位字符，包含字母、数字、下划线'
    },
    confirmPwd: {
      required: '请输入确认密码',
      invalid: '两次输入密码不一致'
    },
    code: {
      required: '请输入验证码',
      invalid: '验证码格式不正确'
    },
    email: {
      required: '请输入邮箱',
      invalid: '邮箱格式不正确'
    }
  },
  dropdown: {
    closeCurrent: '关闭',
    closeOther: '关闭其它',
    closeLeft: '关闭左侧',
    closeRight: '关闭右侧',
    closeAll: '关闭所有',
    pin: '固定标签',
    unpin: '取消固定'
  },
  icon: {
    themeConfig: '主题配置',
    themeSchema: '主题模式',
    lang: '切换语言',
    fullscreen: '全屏',
    fullscreenExit: '退出全屏',
    reload: '刷新页面',
    collapse: '折叠菜单',
    expand: '展开菜单',
    pin: '固定',
    unpin: '取消固定'
  },
  datatable: {
    itemCount: '共 {total} 条',
    fixed: {
      left: '左固定',
      right: '右固定',
      unFixed: '取消固定'
    }
  }
};

export default local;
