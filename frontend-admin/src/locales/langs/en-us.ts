import { enUSErrorMessages } from './v212-error-messages';

const local: App.I18n.Schema = {
  system: {
    title: 'MMMail',
    updateTitle: 'System Version Update Notification',
    updateContent: 'A new version of the system has been detected. Do you want to refresh the page immediately?',
    updateConfirm: 'Refresh immediately',
    updateCancel: 'Later'
  },
  common: {
    action: 'Action',
    add: 'Add',
    addSuccess: 'Add Success',
    backToHome: 'Back to home',
    batchDelete: 'Batch Delete',
    cancel: 'Cancel',
    close: 'Close',
    check: 'Check',
    select: 'Select',
    selectAll: 'Select All',
    expandColumn: 'Expand Column',
    columnSetting: 'Column Setting',
    config: 'Config',
    confirm: 'Confirm',
    delete: 'Delete',
    deleteSuccess: 'Delete Success',
    confirmDelete: 'Are you sure you want to delete?',
    edit: 'Edit',
    warning: 'Warning',
    error: 'Error',
    index: 'Index',
    keywordSearch: 'Please enter keyword',
    logout: 'Logout',
    logoutConfirm: 'Are you sure you want to log out?',
    lookForward: 'Coming soon',
    modify: 'Modify',
    modifySuccess: 'Modify Success',
    noData: 'No Data',
    operate: 'Operate',
    pleaseCheckValue: 'Please check whether the value is valid',
    refresh: 'Refresh',
    reset: 'Reset',
    search: 'Search',
    switch: 'Switch',
    tip: 'Tip',
    trigger: 'Trigger',
    update: 'Update',
    updateSuccess: 'Update Success',
    userCenter: 'User Center',
    yesOrNo: {
      yes: 'Yes',
      no: 'No'
    }
  },
  request: {
    logout: 'Logout user after request failed',
    logoutMsg: 'User status is invalid, please log in again',
    logoutWithModal: 'Pop up modal after request failed and then log out user',
    logoutWithModalMsg: 'User status is invalid, please log in again',
    refreshToken: 'The requested token has expired, refresh the token',
    tokenExpired: 'The requested token has expired',
    traceId: 'Trace ID',
    copyDetails: 'Copy details',
    copyDetailsSuccess: 'Error details copied',
    copyDetailsFailed: 'Failed to copy error details'
  },
  errors: enUSErrorMessages,
  theme: {
    themeDrawerTitle: 'Theme Configuration',
    tabs: {
      appearance: 'Appearance',
      layout: 'Layout',
      general: 'General',
      preset: 'Preset'
    },
    appearance: {
      themeSchema: {
        title: 'Theme Schema',
        light: 'Light',
        dark: 'Dark',
        auto: 'Follow System'
      },
      grayscale: 'Grayscale',
      colourWeakness: 'Colour Weakness',
      themeColor: {
        title: 'Theme Color',
        primary: 'Primary',
        info: 'Info',
        success: 'Success',
        warning: 'Warning',
        error: 'Error',
        followPrimary: 'Follow Primary'
      },
      themeRadius: {
        title: 'Theme Radius'
      },
      recommendColor: 'Apply Recommended Color Algorithm',
      recommendColorDesc: 'The recommended color algorithm refers to',
      preset: {
        title: 'Theme Presets',
        apply: 'Apply',
        applySuccess: 'Preset applied successfully',
        default: {
          name: 'Default Preset',
          desc: 'Default theme preset with balanced settings'
        },
        dark: {
          name: 'Dark Preset',
          desc: 'Dark theme preset for night time usage'
        },
        compact: {
          name: 'Compact Preset',
          desc: 'Compact layout preset for small screens'
        },
        azir: {
          name: "Azir's Preset",
          desc: 'It is a cold and elegant preset that Azir likes'
        }
      }
    },
    layout: {
      layoutMode: {
        title: 'Layout Mode',
        vertical: 'Vertical Mode',
        horizontal: 'Horizontal Mode',
        'vertical-mix': 'Vertical Mix Mode',
        'vertical-hybrid-header-first': 'Left Hybrid Header-First',
        'top-hybrid-sidebar-first': 'Top-Hybrid Sidebar-First',
        'top-hybrid-header-first': 'Top-Hybrid Header-First',
        vertical_detail: 'Vertical menu layout, with the menu on the left and content on the right.',
        'vertical-mix_detail':
          'Vertical mix-menu layout, with the primary menu on the dark left side and the secondary menu on the lighter left side.',
        'vertical-hybrid-header-first_detail':
          'Left hybrid layout, with the primary menu at the top, the secondary menu on the dark left side, and the tertiary menu on the lighter left side.',
        horizontal_detail: 'Horizontal menu layout, with the menu at the top and content below.',
        'top-hybrid-sidebar-first_detail':
          'Top hybrid layout, with the primary menu on the left and the secondary menu at the top.',
        'top-hybrid-header-first_detail':
          'Top hybrid layout, with the primary menu at the top and the secondary menu on the left.'
      },
      tab: {
        title: 'Tab Settings',
        visible: 'Tab Visible',
        cache: 'Tag Bar Info Cache',
        cacheTip: 'Keep the tab bar information after leaving the page',
        height: 'Tab Height',
        mode: {
          title: 'Tab Mode',
          slider: 'Slider',
          chrome: 'Chrome',
          button: 'Button'
        },
        closeByMiddleClick: 'Close Tab by Middle Click',
        closeByMiddleClickTip: 'Enable closing tabs by clicking with the middle mouse button'
      },
      header: {
        title: 'Header Settings',
        height: 'Header Height',
        breadcrumb: {
          visible: 'Breadcrumb Visible',
          showIcon: 'Breadcrumb Icon Visible'
        }
      },
      sider: {
        title: 'Sider Settings',
        inverted: 'Dark Sider',
        width: 'Sider Width',
        collapsedWidth: 'Sider Collapsed Width',
        mixWidth: 'Mix Sider Width',
        mixCollapsedWidth: 'Mix Sider Collapse Width',
        mixChildMenuWidth: 'Mix Child Menu Width',
        autoSelectFirstMenu: 'Auto Select First Submenu',
        autoSelectFirstMenuTip:
          'When a first-level menu is clicked, the first submenu is automatically selected and navigated to the deepest level'
      },
      footer: {
        title: 'Footer Settings',
        visible: 'Footer Visible',
        fixed: 'Fixed Footer',
        height: 'Footer Height',
        right: 'Right Footer'
      },
      content: {
        title: 'Content Area Settings',
        scrollMode: {
          title: 'Scroll Mode',
          tip: 'The theme scroll only scrolls the main part, the outer scroll can carry the header and footer together',
          wrapper: 'Wrapper',
          content: 'Content'
        },
        page: {
          animate: 'Page Animate',
          mode: {
            title: 'Page Animate Mode',
            fade: 'Fade',
            'fade-slide': 'Slide',
            'fade-bottom': 'Fade Zoom',
            'fade-scale': 'Fade Scale',
            'zoom-fade': 'Zoom Fade',
            'zoom-out': 'Zoom Out',
            none: 'None'
          }
        },
        fixedHeaderAndTab: 'Fixed Header And Tab'
      }
    },
    general: {
      title: 'General Settings',
      watermark: {
        title: 'Watermark Settings',
        visible: 'Watermark Full Screen Visible',
        text: 'Custom Watermark Text',
        enableUserName: 'Enable User Name Watermark',
        enableTime: 'Show Current Time',
        timeFormat: 'Time Format'
      },
      multilingual: {
        title: 'Multilingual Settings',
        visible: 'Display multilingual button'
      },
      globalSearch: {
        title: 'Global Search Settings',
        visible: 'Display GlobalSearch button'
      }
    },
    configOperation: {
      copyConfig: 'Copy Config',
      copySuccessMsg: 'Copy Success, Please replace the variable "themeSettings" in "src/theme/settings.ts"',
      resetConfig: 'Reset Config',
      resetSuccessMsg: 'Reset Success'
    }
  },
  route: {
    login: 'Login',
    403: 'No Permission',
    404: 'Page Not Found',
    500: 'Server Error',
    'iframe-page': 'Iframe',
    home: 'Home',
    'business-overview': 'Business overview',
    mail: 'Mail',
    calendar: 'Calendar',
    drive: 'Drive',
    docs: 'Docs',
    sheets: 'Sheets',
    pass: 'Pass',
    collaboration: 'Collaboration',
    'command-center': 'Command Center',
    notifications: 'Notifications',
    admin: 'Admin',
    settings: 'Settings',
    share: 'Public share',
    community: 'Community',
    contacts: 'Contacts',
    wallet: 'Wallet',
    vpn: 'VPN',
    meet: 'Meet',
    integrations: 'Integrations',
    integrations_simplelogin: 'SimpleLogin',
    notes: 'Notes',
    security: 'Security',
    search: 'Search',
    security_authenticator: 'Authenticator'
  },
  page: {
    state: {
      loading: { description: 'Loading…' },
      empty: { description: 'No data' },
      error: { title: 'Loading failed', description: 'Please retry later or contact an admin.' }
    },
    businessOverview: {
      statsTitle: 'Organization snapshot',
      teamSpacesTitle: 'Team spaces',
      memberCount: 'Members',
      adminCount: 'Admins',
      pendingInviteCount: 'Pending invites',
      teamSpaceCount: 'Team spaces',
      storage: 'Storage usage',
      governanceSla: 'Review SLA',
      dualReview: 'Dual review',
      openSpace: 'Open',
      download: 'Download',
      column: {
        name: 'Name',
        role: 'Role',
        itemCount: 'Items',
        storage: 'Storage',
        updatedAt: 'Updated'
      }
    },
    accessGate: {
      upgrade: {
        title: 'Upgrade to unlock this feature',
        description: 'The current organization does not include the required entitlement.',
        primary: 'Upgrade now',
        secondary: 'Learn more'
      },
      'contact-sales': {
        title: 'Contact sales required',
        description: 'This capability is intended for advanced organization workflows.',
        primary: 'Contact sales',
        secondary: 'Join waitlist'
      },
      trial: {
        title: 'Start a 14-day trial',
        description: 'Try the full capability without changing existing data.',
        primary: 'Start trial',
        secondary: 'View terms'
      },
      forbidden: {
        title: 'Forbidden',
        description: 'Your account does not have permission to access this page.',
        primary: 'Back home',
        secondary: 'Contact admin'
      }
    },
    login: {
      common: {
        loginOrRegister: 'Login / Register',
        userNamePlaceholder: 'Please enter user name',
        emailPlaceholder: 'Please enter email',
        phonePlaceholder: 'Please enter phone number',
        codePlaceholder: 'Please enter verification code',
        passwordPlaceholder: 'Please enter password',
        confirmPasswordPlaceholder: 'Please enter password again',
        codeLogin: 'Verification code login',
        confirm: 'Confirm',
        back: 'Back',
        loginSuccess: 'Login successfully',
        welcomeBack: 'Welcome back, {userName} !',
        headline: 'Enter the MMMail workspace',
        subtitle: 'Use one identity for mail, drive, calendar, and administration.'
      },
      pwdLogin: {
        title: 'Password Login',
        rememberMe: 'Remember me',
        forgetPassword: 'Forget password?',
        register: 'Register',
        otherAccountLogin: 'Other Account Login',
        otherLoginMode: 'Other Login Mode',
        superAdmin: 'Super Admin',
        admin: 'Admin',
        user: 'User'
      },
      codeLogin: {
        title: 'Verification Code Login',
        getCode: 'Get verification code',
        reGetCode: 'Reacquire after {time}s',
        imageCodePlaceholder: 'Please enter image verification code',
        unavailable: 'Verification code login is not connected to the backend. Use email and password login.'
      },
      register: {
        title: 'Create account',
        submit: 'Create account',
        agreement: 'I have read and agree to',
        protocol: '《User Agreement》',
        policy: '《Privacy Policy》'
      },
      security: {
        title: 'Security verification required',
        secondFactorRequired: 'New sign-in location detected. Complete second-factor verification to continue.',
        lockTitle: 'Account temporarily locked',
        lockCountdown: 'Too many failed sign-in attempts. Try again after {time}.'
      },
      resetPwd: {
        title: 'Reset Password',
        unavailable: 'Password reset is not connected to the backend. Contact an administrator.'
      },
      bindWeChat: {
        title: 'Bind WeChat'
      }
    },
    mail: {
      compose: 'Compose',
      sender: 'Sender',
      subject: 'Subject',
      preview: 'Preview',
      time: 'Time',
      to: 'To',
      body: 'Body',
      send: 'Send',
      saveDraft: 'Save draft',
      bulkMarkRead: 'Mark read',
      bulkDelete: 'Bulk delete',
      selectedCount: '{count} selected',
      reader: 'Mail reader',
      externalAccounts: 'External accounts',
      provider: 'Provider',
      authMode: 'Auth mode',
      accountEmail: 'Account email',
      username: 'Username',
      password: 'App password',
      oauthToken: 'OAuth refresh token',
      imapHost: 'IMAP host',
      imapPort: 'IMAP port',
      smtpHost: 'SMTP host',
      smtpPort: 'SMTP port',
      addAccount: 'Save account',
      testConnection: 'Test connection',
      sync: 'Sync',
      deleteAccount: 'Delete account'
    },
    calendar: {
      create: 'Create event',
      title: 'Title',
      location: 'Location',
      startAt: 'Start',
      endAt: 'End',
      rrule: 'Repeat rule',
      attendees: 'Attendees',
      availability: 'Availability',
      conflict: 'Time conflict detected',
      noConflict: 'This time is available',
      settings: 'Calendar settings',
      deleteSelected: 'Delete selected',
      subscriptions: 'Calendar subscriptions',
      subscriptionLabel: 'Subscription name',
      sourceUrl: 'Subscription URL',
      status: 'Status',
      updatedAt: 'Updated at',
      color: 'Color',
      sync: 'Sync',
      exportIcs: 'Export ICS'
    },
    drive: {
      upload: 'Upload',
      storage: 'Storage',
      folders: 'Folders',
      share: 'Share',
      permission: 'Permission',
      deleteSelected: 'Delete selected',
      fileName: 'File name',
      sizeBytes: 'Size',
      updatedAt: 'Updated at'
    },
    driveSecureShare: {
      title: 'Encrypted share',
      encryptedCopy: 'Encrypted copy',
      sharePassword: 'Access password',
      localKey: 'Local key fragment',
      localKeyWarning: 'The key fragment is never sent to the server and is appended after #k= in the share link.',
      secureLink: 'Secure link',
      create: 'Create encrypted share'
    },
    publicShare: {
      title: 'Public share',
      password: 'Access password',
      localKeyNotice: 'The key is only used locally in this browser and is never sent with API requests.',
      download: 'Download file',
      encrypted: 'End-to-end encrypted'
    },
    workspace: {
      systemStatus: 'System status',
      recommendations: 'Recommendations',
      activity: 'Activity',
      tasks: 'Tasks',
      completed: 'Completed'
    },
    settings: {
      profile: 'Profile',
      security: 'Security',
      devices: 'Devices',
      notifications: 'Notification preferences',
      displayName: 'Display name',
      signature: 'Signature',
      timezone: 'Timezone',
      preferredLocale: 'Preferred locale',
      mailAddressMode: 'Mail address mode',
      autoSaveSeconds: 'Auto save seconds',
      undoSendSeconds: 'Undo send seconds',
      recoveryEmail: 'Recovery email',
      mfaEnabled: 'Enable MFA',
      emailDigest: 'Email digest',
      productUpdates: 'Product updates',
      revokeDevice: 'Revoke device'
    },
    notifications: {
      title: 'Title',
      body: 'Body',
      product: 'Product',
      severity: 'Severity',
      status: 'Status',
      createdAt: 'Created at',
      markRead: 'Mark read',
      archive: 'Archive',
      subscriptions: 'Subscriptions',
      unread: 'Unread'
    },
    domains: {
      title: 'Custom domains',
      domain: 'Domain',
      token: 'Verification token',
      verify: 'Verify domain',
      setDefault: 'Set default',
      diagnostics: 'DNS diagnostics',
      type: 'Type',
      host: 'Host',
      expected: 'Expected',
      actual: 'Current',
      matched: 'Matched'
    },
    webPush: {
      title: 'Web Push',
      endpoint: 'Push endpoint',
      p256dh: 'P-256 DH public key',
      auth: 'Auth secret',
      label: 'Label',
      userAgent: 'User Agent',
      register: 'Register subscription',
      test: 'Test push'
    },
    docs: {
      title: 'Document title',
      content: 'Content',
      create: 'Create doc',
      save: 'Save doc'
    },
    sheets: {
      title: 'Workbook title',
      rows: 'Rows',
      columns: 'Columns',
      create: 'Create workbook',
      saveCell: 'Save cell',
      formula: 'Formula',
      evaluate: 'Evaluate',
      recalculate: 'Recalculate',
      value: 'Value',
      dependencies: 'Dependencies',
      dependents: 'Dependents',
      dependencyGraph: 'Dependency graph'
    },
    pass: {
      title: 'Item title',
      website: 'Website',
      username: 'Username',
      secret: 'Secret',
      create: 'Create item',
      monitor: 'Risk monitor'
    },
    collaboration: {
      title: 'Collaboration title',
      product: 'Product',
      status: 'Status',
      create: 'Create project',
      tasks: 'Tasks'
    },
    commandCenter: {
      title: 'Command',
      description: 'Description',
      product: 'Product',
      enabled: 'Enabled',
      catalog: 'Catalog',
      recents: 'Recents',
      systemCommands: 'System commands',
      search: 'Quick search',
      searchPlaceholder: 'Search commands, mail, files, or secure items',
      group: 'Group',
      shortcut: 'Shortcut',
      route: 'Route',
      pinned: 'Pinned',
      execute: 'Run',
      pin: 'Pin',
      unpin: 'Unpin',
      source: 'Source',
      usageCount: 'Usage count'
    },
    admin: {
      title: 'Admin',
      domains: 'Domains',
      productAccess: 'Product access',
      sessions: 'Member sessions',
      members: 'Members'
    },
    contacts: {
      title: 'Contacts',
      name: 'Name',
      email: 'Email',
      note: 'Note',
      create: 'Create contact',
      groups: 'Groups'
    },
    wallet: {
      title: 'Wallet',
      account: 'Account',
      asset: 'Asset',
      balance: 'Balance',
      transaction: 'Transaction',
      type: 'Type',
      amount: 'Amount',
      execution: 'Execution overview',
      health: 'Health score',
      transactions: 'Transactions',
      accounts: 'Accounts',
      send: 'Send',
      receive: 'Receive',
      reconciliation: 'Reconciliation',
      transactionDetail: 'Transaction detail',
      createAccount: 'Create account',
      executionPlan: 'Execution plan',
      pendingActions: 'Pending actions',
      trace: 'Execution trace',
      integrity: 'Integrity',
      risk: 'Risk',
      blocked: 'Blocked',
      mismatch: 'Mismatch',
      sourceAddress: 'Source address',
      targetAddress: 'Target address',
      memo: 'Memo',
      networkTxHash: 'Network Tx Hash',
      signerHint: 'Signer',
      operatorHint: 'Operator',
      address: 'Address',
      confirmations: 'Confirmations',
      maxItems: 'Max items',
      strategy: 'Strategy',
      reason: 'Reason',
      action: 'Action',
      sign: 'Sign',
      broadcast: 'Broadcast',
      confirm: 'Confirm',
      remediate: 'Remediate',
      fail: 'Mark failed',
      batchAdvance: 'Batch advance',
      batchRemediate: 'Batch remediate',
      batchReconcile: 'Batch reconcile'
    },
    vpn: {
      title: 'VPN',
      country: 'Country',
      city: 'City',
      tier: 'Tier',
      load: 'Load',
      session: 'Session',
      sessions: 'Sessions',
      servers: 'Servers',
      profiles: 'Profiles',
      settings: 'Settings',
      name: 'Name',
      protocol: 'Protocol',
      routingMode: 'Routing mode',
      targetServer: 'Target server',
      targetCountry: 'Target country',
      secureCore: 'Secure core',
      netshield: 'NetShield',
      killSwitch: 'Kill switch',
      defaultMode: 'Default mode',
      connect: 'Connect',
      history: 'History',
      createProfile: 'Create profile',
      updateProfile: 'Update profile',
      quickConnect: 'Quick connect',
      disconnect: 'Disconnect'
    },
    meet: {
      title: 'Meet',
      topic: 'Topic',
      accessLevel: 'Access level',
      startedAt: 'Started at',
      plan: 'Plan',
      current: 'Current room',
      create: 'Create room',
      maxParticipants: 'Max participants',
      access: 'Access',
      rooms: 'Rooms',
      room: 'Room',
      lobby: 'Lobby',
      join: 'Join',
      host: 'Host',
      waitlist: 'Waitlist',
      activate: 'Activate',
      contactSales: 'Contact sales',
      company: 'Company',
      seats: 'Seats',
      note: 'Note',
      history: 'History',
      participants: 'Participants',
      guestRequests: 'Guest requests',
      signals: 'Signals',
      quality: 'Quality',
      displayName: 'Display name',
      joinCode: 'Join code',
      requestToken: 'Request token',
      guestSession: 'Guest session',
      audio: 'Audio',
      video: 'Video',
      screen: 'Screen',
      approve: 'Approve',
      reject: 'Reject',
      rotateJoinCode: 'Rotate join code',
      endRoom: 'End room',
      heartbeat: 'Heartbeat',
      payload: 'Payload',
      jitter: 'Jitter',
      packetLoss: 'Packet loss',
      roundTrip: 'Round trip'
    },
    authenticator: {
      title: 'Authenticator',
      issuer: 'Issuer',
      account: 'Account',
      algorithm: 'Algorithm',
      secret: 'Secret ciphertext',
      create: 'Create authenticator',
      generate: 'Generate code',
      sync: 'Sync',
      import: 'Import',
      backup: 'Backup',
      settings: 'Security settings',
      pin: 'PIN',
      qrImage: 'QR image',
      exported: 'Exported content',
      lockTimeout: 'Lock timeout'
    },
    community: {
      title: 'Community',
      posts: 'Posts',
      newPost: 'New post',
      topic: 'Topic',
      postTitle: 'Title',
      tags: 'Tags',
      body: 'Body',
      publish: 'Publish',
      empty: 'No community content',
      comment: 'Comment',
      send: 'Send',
      report: 'Report',
      bookmark: 'Bookmark',
      like: 'Like'
    },
    simpleLogin: {
      title: 'SimpleLogin',
      aliases: 'Aliases',
      enabled: 'Enabled',
      domains: 'Verified domains',
      domain: 'Domain',
      defaultMailbox: 'Default mailbox',
      defaultMailboxId: 'Default mailbox ID',
      customDomainId: 'Custom domain ID',
      subdomainMode: 'Subdomain mode',
      catchAll: 'Catch-all',
      createPolicy: 'Create relay policy'
    },
    standardNotes: {
      title: 'Notes',
      folders: 'Folders',
      total: 'Total notes',
      checklist: 'Checklist tasks',
      exported: 'Exported notes',
      editor: 'Editor',
      noteTitle: 'Note title',
      type: 'Type',
      folder: 'Folder',
      tags: 'Tags',
      create: 'Create note',
      toggleChecklist: 'Toggle first checklist item',
      export: 'Export'
    },
    mailFilters: {
      title: 'Mail filters',
      name: 'Rule name',
      targetFolder: 'Target folder',
      labels: 'Labels',
      markRead: 'Mark read',
      create: 'Create rule',
      preview: 'Preview rule'
    },
    driveVersions: {
      title: 'Version history',
      version: 'Version',
      author: 'Author',
      compare: 'Compare',
      checksum: 'Checksum',
      createdAt: 'Created at',
      restoreConfirm: 'Restoring creates a new current version. Continue?',
      restore: 'Restore version',
      e2eeReady: 'Encrypted sharing capability ready'
    },
    billing: {
      title: 'Billing',
      plan: 'Current plan',
      subscriptions: 'Subscriptions',
      invoices: 'Invoices',
      paymentMethods: 'Payment methods',
      offers: 'Offers',
      invoice: 'Invoice',
      offer: 'Offer',
      quote: 'Quote preview',
      checkoutDraft: 'Draft',
      billingCycle: 'Billing cycle',
      seatCount: 'Seats',
      defaultPayment: 'Default payment method',
      total: 'Total',
      runAction: 'Run subscription action',
      addPaymentMethod: 'Add payment method',
      createQuote: 'Create quote',
      createDraft: 'Create draft'
    }
  },
  form: {
    required: 'Cannot be empty',
    userName: {
      required: 'Please enter user name',
      invalid: 'User name format is incorrect'
    },
    phone: {
      required: 'Please enter phone number',
      invalid: 'Phone number format is incorrect'
    },
    pwd: {
      required: 'Please enter password',
      invalid: '6-18 characters, including letters, numbers, and underscores'
    },
    confirmPwd: {
      required: 'Please enter password again',
      invalid: 'The two passwords are inconsistent'
    },
    code: {
      required: 'Please enter verification code',
      invalid: 'Verification code format is incorrect'
    },
    email: {
      required: 'Please enter email',
      invalid: 'Email format is incorrect'
    }
  },
  dropdown: {
    closeCurrent: 'Close Current',
    closeOther: 'Close Other',
    closeLeft: 'Close Left',
    closeRight: 'Close Right',
    closeAll: 'Close All',
    pin: 'Pin Tab',
    unpin: 'Unpin Tab'
  },
  icon: {
    themeConfig: 'Theme Configuration',
    themeSchema: 'Theme Schema',
    lang: 'Switch Language',
    fullscreen: 'Fullscreen',
    fullscreenExit: 'Exit Fullscreen',
    reload: 'Reload Page',
    collapse: 'Collapse Menu',
    expand: 'Expand Menu',
    pin: 'Pin',
    unpin: 'Unpin'
  },
  datatable: {
    itemCount: 'Total {total} items',
    fixed: {
      left: 'Left Fixed',
      right: 'Right Fixed',
      unFixed: 'Unfixed'
    }
  }
};

export default local;
