/** The global namespace for the app */
declare namespace App {
  /** Theme namespace */
  namespace Theme {
    type ColorPaletteNumber = import('@sa/color').ColorPaletteNumber;

    /** NaiveUI theme overrides that can be specified in preset */
    type NaiveUIThemeOverride = import('naive-ui').GlobalThemeOverrides;

    /** Theme setting */
    interface ThemeSetting {
      /** Theme scheme */
      themeScheme: UnionKey.ThemeScheme;
      /** grayscale mode */
      grayscale: boolean;
      /** colour weakness mode */
      colourWeakness: boolean;
      /** Whether to recommend color */
      recommendColor: boolean;
      /** Theme color */
      themeColor: string;
      /** Theme radius */
      themeRadius: number;
      /** Other color */
      otherColor: OtherColor;
      /** Whether info color is followed by the primary color */
      isInfoFollowPrimary: boolean;
      /** Layout */
      layout: {
        /** Layout mode */
        mode: UnionKey.ThemeLayoutMode;
        /** Scroll mode */
        scrollMode: UnionKey.ThemeScrollMode;
      };
      /** Page */
      page: {
        /** Whether to show the page transition */
        animate: boolean;
        /** Page animate mode */
        animateMode: UnionKey.ThemePageAnimateMode;
      };
      /** Header */
      header: {
        /** Header height */
        height: number;
        /** Header breadcrumb */
        breadcrumb: {
          /** Whether to show the breadcrumb */
          visible: boolean;
          /** Whether to show the breadcrumb icon */
          showIcon: boolean;
        };
        /** Multilingual */
        multilingual: {
          /** Whether to show the multilingual */
          visible: boolean;
        };
        globalSearch: {
          /** Whether to show the GlobalSearch */
          visible: boolean;
        };
      };
      /** Tab */
      tab: {
        /** Whether to show the tab */
        visible: boolean;
        /**
         * Whether to cache the tab
         *
         * If cache, the tabs will get from the local storage when the page is refreshed
         */
        cache: boolean;
        /** Tab height */
        height: number;
        /** Tab mode */
        mode: UnionKey.ThemeTabMode;
        /** Whether to close tab by middle click */
        closeTabByMiddleClick: boolean;
      };
      /** Fixed header and tab */
      fixedHeaderAndTab: boolean;
      /** Sider */
      sider: {
        /** Inverted sider */
        inverted: boolean;
        /** Sider width */
        width: number;
        /** Collapsed sider width */
        collapsedWidth: number;
        /** Sider width when the layout is 'vertical-mix', 'top-hybrid-sidebar-first', or 'top-hybrid-header-first' */
        mixWidth: number;
        /**
         * Collapsed sider width when the layout is 'vertical-mix', 'top-hybrid-sidebar-first', or
         * 'top-hybrid-header-first'
         */
        mixCollapsedWidth: number;
        /** Child menu width when the layout is 'vertical-mix', 'top-hybrid-sidebar-first', or 'top-hybrid-header-first' */
        mixChildMenuWidth: number;
        /** Whether to auto select the first submenu */
        autoSelectFirstMenu: boolean;
      };
      /** Footer */
      footer: {
        /** Whether to show the footer */
        visible: boolean;
        /** Whether fixed the footer */
        fixed: boolean;
        /** Footer height */
        height: number;
        /**
         * Whether float the footer to the right when the layout is 'top-hybrid-sidebar-first' or
         * 'top-hybrid-header-first'
         */
        right: boolean;
      };
      /** Watermark */
      watermark: {
        /** Whether to show the watermark */
        visible: boolean;
        /** Watermark text */
        text: string;
        /** Whether to use user name as watermark text */
        enableUserName: boolean;
        /** Whether to use current time as watermark text */
        enableTime: boolean;
        /** Time format for watermark text */
        timeFormat: string;
      };
      /** define some theme settings tokens, will transform to css variables */
      tokens: {
        light: ThemeSettingToken;
        dark?: {
          [K in keyof ThemeSettingToken]?: Partial<ThemeSettingToken[K]>;
        };
      };
    }

    interface OtherColor {
      info: string;
      success: string;
      warning: string;
      error: string;
    }

    interface ThemeColor extends OtherColor {
      primary: string;
    }

    type ThemeColorKey = keyof ThemeColor;

    type ThemePaletteColor = {
      [key in ThemeColorKey | `${ThemeColorKey}-${ColorPaletteNumber}`]: string;
    };

    type BaseToken = Record<string, Record<string, string>>;

    interface ThemeSettingTokenColor {
      /** the progress bar color, if not set, will use the primary color */
      nprogress?: string;
      container: string;
      layout: string;
      inverted: string;
      'base-text': string;
    }

    interface ThemeSettingTokenBoxShadow {
      header: string;
      sider: string;
      tab: string;
    }

    interface ThemeSettingToken {
      colors: ThemeSettingTokenColor;
      boxShadow: ThemeSettingTokenBoxShadow;
    }

    type ThemeTokenColor = ThemePaletteColor & ThemeSettingTokenColor;

    /** Theme token CSS variables */
    type ThemeTokenCSSVars = {
      colors: ThemeTokenColor & { [key: string]: string };
      boxShadow: ThemeSettingTokenBoxShadow & { [key: string]: string };
    };
  }

  /** Global namespace */
  namespace Global {
    type VNode = import('vue').VNode;
    type RouteLocationNormalizedLoaded = import('vue-router').RouteLocationNormalizedLoaded;
    type RouteKey = import('@elegant-router/types').RouteKey;
    type RouteMap = import('@elegant-router/types').RouteMap;
    type RoutePath = import('@elegant-router/types').RoutePath;
    type LastLevelRouteKey = import('@elegant-router/types').LastLevelRouteKey;

    /** The router push options */
    type RouterPushOptions = {
      query?: Record<string, string>;
      params?: Record<string, string>;
      force?: boolean;
    };

    /** The global header props */
    interface HeaderProps {
      /** Whether to show the logo */
      showLogo?: boolean;
      /** Whether to show the menu toggler */
      showMenuToggler?: boolean;
      /** Whether to show the menu */
      showMenu?: boolean;
    }

    /** The global menu */
    type Menu = {
      /**
       * The menu key
       *
       * Equal to the route key
       */
      key: string;
      /** The menu label */
      label: string;
      /** The menu i18n key */
      i18nKey?: I18n.I18nKey | null;
      /** The route key */
      routeKey: RouteKey;
      /** The route path */
      routePath: RoutePath;
      /** The menu icon */
      icon?: () => VNode;
      /** The menu children */
      children?: Menu[];
    };

    type Breadcrumb = Omit<Menu, 'children'> & {
      options?: Breadcrumb[];
    };

    /** Tab route */
    type TabRoute = Pick<RouteLocationNormalizedLoaded, 'name' | 'path' | 'meta'> &
      Partial<Pick<RouteLocationNormalizedLoaded, 'fullPath' | 'query' | 'matched'>>;

    /** The global tab */
    type Tab = {
      /** The tab id */
      id: string;
      /** The tab label */
      label: string;
      /**
       * The new tab label
       *
       * If set, the tab label will be replaced by this value
       */
      newLabel?: string;
      /**
       * The old tab label
       *
       * when reset the tab label, the tab label will be replaced by this value
       */
      oldLabel?: string;
      /** The tab route key */
      routeKey: LastLevelRouteKey;
      /** The tab route path */
      routePath: RouteMap[LastLevelRouteKey];
      /** The tab route full path */
      fullPath: string;
      /** The tab fixed index */
      fixedIndex?: number | null;
      /**
       * Tab icon
       *
       * Iconify icon
       */
      icon?: string;
      /**
       * Tab local icon
       *
       * Local icon
       */
      localIcon?: string;
      /** I18n key */
      i18nKey?: I18n.I18nKey | null;
    };

    /** Form rule */
    type FormRule = import('naive-ui').FormItemRule;

    /** The global dropdown key */
    type DropdownKey = 'closeCurrent' | 'closeOther' | 'closeLeft' | 'closeRight' | 'closeAll' | 'pin' | 'unpin';
  }

  /**
   * I18n namespace
   *
   * Locales type
   */
  namespace I18n {
    type RouteKey = import('@elegant-router/types').RouteKey;

    type LangType = 'zh-CN' | 'zh-TW' | 'en-US';

    type LangOption = {
      label: string;
      key: LangType;
    };

    type I18nRouteKey = Exclude<RouteKey, 'root' | 'not-found'>;

    type FormMsg = {
      required: string;
      invalid: string;
    };

    type Schema = {
      system: {
        title: string;
        updateTitle: string;
        updateContent: string;
        updateConfirm: string;
        updateCancel: string;
      };
      common: {
        action: string;
        add: string;
        addSuccess: string;
        backToHome: string;
        batchDelete: string;
        cancel: string;
        close: string;
        check: string;
        select: string;
        selectAll: string;
        expandColumn: string;
        columnSetting: string;
        config: string;
        confirm: string;
        delete: string;
        deleteSuccess: string;
        confirmDelete: string;
        edit: string;
        warning: string;
        error: string;
        index: string;
        keywordSearch: string;
        logout: string;
        logoutConfirm: string;
        lookForward: string;
        modify: string;
        modifySuccess: string;
        noData: string;
        operate: string;
        pleaseCheckValue: string;
        refresh: string;
        reset: string;
        search: string;
        switch: string;
        tip: string;
        trigger: string;
        update: string;
        updateSuccess: string;
        userCenter: string;
        yesOrNo: {
          yes: string;
          no: string;
        };
      };
      request: {
        logout: string;
        logoutMsg: string;
        logoutWithModal: string;
        logoutWithModalMsg: string;
        refreshToken: string;
        tokenExpired: string;
        traceId: string;
        copyDetails: string;
        copyDetailsSuccess: string;
        copyDetailsFailed: string;
      };
      errors: Record<string, { title: string; message: string }>;
      theme: {
        themeDrawerTitle: string;
        tabs: {
          appearance: string;
          layout: string;
          general: string;
          preset: string;
        };
        appearance: {
          themeSchema: { title: string } & Record<UnionKey.ThemeScheme, string>;
          grayscale: string;
          colourWeakness: string;
          themeColor: {
            title: string;
            followPrimary: string;
          } & Record<Theme.ThemeColorKey, string>;
          recommendColor: string;
          recommendColorDesc: string;
          themeRadius: {
            title: string;
          };
          preset: {
            title: string;
            apply: string;
            applySuccess: string;
            [key: string]:
              | {
                  name: string;
                  desc: string;
                }
              | string;
          };
        };
        layout: {
          layoutMode: { title: string } & Record<UnionKey.ThemeLayoutMode, string> & {
              [K in `${UnionKey.ThemeLayoutMode}_detail`]: string;
            };
          tab: {
            title: string;
            visible: string;
            cache: string;
            cacheTip: string;
            height: string;
            mode: { title: string } & Record<UnionKey.ThemeTabMode, string>;
            closeByMiddleClick: string;
            closeByMiddleClickTip: string;
          };
          header: {
            title: string;
            height: string;
            breadcrumb: {
              visible: string;
              showIcon: string;
            };
          };
          sider: {
            title: string;
            inverted: string;
            width: string;
            collapsedWidth: string;
            mixWidth: string;
            mixCollapsedWidth: string;
            mixChildMenuWidth: string;
            autoSelectFirstMenu: string;
            autoSelectFirstMenuTip: string;
          };
          footer: {
            title: string;
            visible: string;
            fixed: string;
            height: string;
            right: string;
          };
          content: {
            title: string;
            scrollMode: { title: string; tip: string } & Record<UnionKey.ThemeScrollMode, string>;
            page: {
              animate: string;
              mode: { title: string } & Record<UnionKey.ThemePageAnimateMode, string>;
            };
            fixedHeaderAndTab: string;
          };
        };
        general: {
          title: string;
          watermark: {
            title: string;
            visible: string;
            text: string;
            enableUserName: string;
            enableTime: string;
            timeFormat: string;
          };
          multilingual: {
            title: string;
            visible: string;
          };
          globalSearch: {
            title: string;
            visible: string;
          };
        };
        configOperation: {
          copyConfig: string;
          copySuccessMsg: string;
          resetConfig: string;
          resetSuccessMsg: string;
        };
      };
      route: Record<I18nRouteKey, string>;
      page: {
        state: {
          loading: { description: string };
          empty: { description: string };
          error: { title: string; description: string };
        };
        businessOverview: {
          statsTitle: string;
          teamSpacesTitle: string;
          memberCount: string;
          adminCount: string;
          pendingInviteCount: string;
          teamSpaceCount: string;
          storage: string;
          governanceSla: string;
          dualReview: string;
          openSpace: string;
          download: string;
          column: {
            name: string;
            role: string;
            itemCount: string;
            storage: string;
            updatedAt: string;
          };
        };
        accessGate: {
          upgrade: {
            title: string;
            description: string;
            primary: string;
            secondary: string;
          };
          'contact-sales': {
            title: string;
            description: string;
            primary: string;
            secondary: string;
          };
          trial: {
            title: string;
            description: string;
            primary: string;
            secondary: string;
          };
          forbidden: {
            title: string;
            description: string;
            primary: string;
            secondary: string;
          };
          backendEntitlement: {
            requiredEdition: string;
            currentEdition: string;
            upgradeAction: string;
          };
        };
        login: {
          common: {
            loginOrRegister: string;
            userNamePlaceholder: string;
            emailPlaceholder: string;
            phonePlaceholder: string;
            codePlaceholder: string;
            passwordPlaceholder: string;
            confirmPasswordPlaceholder: string;
            codeLogin: string;
            confirm: string;
            back: string;
            loginSuccess: string;
            welcomeBack: string;
            headline: string;
            subtitle: string;
          };
          pwdLogin: {
            title: string;
            rememberMe: string;
            forgetPassword: string;
            register: string;
            otherAccountLogin: string;
            otherLoginMode: string;
            superAdmin: string;
            admin: string;
            user: string;
          };
          codeLogin: {
            title: string;
            getCode: string;
            reGetCode: string;
            imageCodePlaceholder: string;
            unavailable: string;
          };
          register: {
            title: string;
            submit: string;
            agreement: string;
            protocol: string;
            policy: string;
          };
          security: {
            title: string;
            secondFactorRequired: string;
            lockTitle: string;
            lockCountdown: string;
          };
          resetPwd: {
            title: string;
            unavailable: string;
          };
          bindWeChat: {
            title: string;
          };
        };
        mail: {
          compose: string;
          sender: string;
          subject: string;
          preview: string;
          time: string;
          to: string;
          body: string;
          send: string;
          saveDraft: string;
          bulkMarkRead: string;
          bulkDelete: string;
          selectedCount: string;
          reader: string;
          externalAccounts: string;
          provider: string;
          authMode: string;
          accountEmail: string;
          username: string;
          password: string;
          oauthToken: string;
          imapHost: string;
          imapPort: string;
          smtpHost: string;
          smtpPort: string;
          addAccount: string;
          testConnection: string;
          sync: string;
          deleteAccount: string;
        };
        calendar: {
          create: string;
          title: string;
          location: string;
          startAt: string;
          endAt: string;
          rrule: string;
          attendees: string;
          availability: string;
          conflict: string;
          noConflict: string;
          settings: string;
          deleteSelected: string;
          subscriptions: string;
          subscriptionLabel: string;
          sourceUrl: string;
          status: string;
          updatedAt: string;
          color: string;
          sync: string;
          exportIcs: string;
        };
        drive: {
          upload: string;
          storage: string;
          folders: string;
          share: string;
          permission: string;
          deleteSelected: string;
          fileName: string;
          sizeBytes: string;
          updatedAt: string;
        };
        driveSecureShare: {
          title: string;
          encryptedCopy: string;
          sharePassword: string;
          localKey: string;
          localKeyWarning: string;
          secureLink: string;
          create: string;
        };
        publicShare: {
          title: string;
          password: string;
          passwordHint: string;
          localKeyNotice: string;
          download: string;
          encrypted: string;
        };
        workspace: {
          systemStatus: string;
          recommendations: string;
          activity: string;
          tasks: string;
          completed: string;
        };
        settings: {
          profile: string;
          security: string;
          devices: string;
          notifications: string;
          displayName: string;
          signature: string;
          timezone: string;
          preferredLocale: string;
          mailAddressMode: string;
          autoSaveSeconds: string;
          undoSendSeconds: string;
          recoveryEmail: string;
          mfaEnabled: string;
          emailDigest: string;
          productUpdates: string;
          revokeDevice: string;
        };
        license: {
          title: string;
          state: string;
          noEdition: string;
          features: string;
          externalBillingStatus: string;
          expiresAt: string;
          syncedAt: string;
          licenseKey: string;
          licenseKeyPlaceholder: string;
          licenseKeyRequired: string;
          upload: string;
          uploadAccepted: string;
          refresh: string;
        };
        oidc: {
          title: string;
          configure: string;
          unavailableTitle: string;
          unavailableDescription: string;
        };
        notifications: {
          title: string;
          body: string;
          product: string;
          severity: string;
          status: string;
          createdAt: string;
          markRead: string;
          archive: string;
          subscriptions: string;
          unread: string;
        };
        domains: {
          title: string;
          domain: string;
          token: string;
          verify: string;
          setDefault: string;
          diagnostics: string;
          type: string;
          host: string;
          expected: string;
          actual: string;
          matched: string;
        };
        webPush: {
          title: string;
          endpoint: string;
          p256dh: string;
          auth: string;
          label: string;
          userAgent: string;
          register: string;
          test: string;
        };
        docs: {
          title: string;
          content: string;
          create: string;
          save: string;
        };
        sheets: {
          title: string;
          rows: string;
          columns: string;
          create: string;
          saveCell: string;
          formula: string;
          evaluate: string;
          recalculate: string;
          value: string;
          dependencies: string;
          dependents: string;
          dependencyGraph: string;
        };
        pass: {
          title: string;
          website: string;
          username: string;
          secret: string;
          create: string;
          monitor: string;
        };
        collaboration: {
          title: string;
          product: string;
          status: string;
          create: string;
          tasks: string;
        };
        commandCenter: {
          title: string;
          description: string;
          product: string;
          enabled: string;
          catalog: string;
          recents: string;
          systemCommands: string;
          search: string;
          searchPlaceholder: string;
          group: string;
          shortcut: string;
          route: string;
          pinned: string;
          execute: string;
          pin: string;
          unpin: string;
          source: string;
          usageCount: string;
        };
        admin: {
          title: string;
          domains: string;
          productAccess: string;
          sessions: string;
          members: string;
        };
        contacts: {
          title: string;
          name: string;
          email: string;
          note: string;
          create: string;
          groups: string;
        };
        wallet: {
          title: string;
          account: string;
          asset: string;
          balance: string;
          transaction: string;
          type: string;
          amount: string;
          execution: string;
          health: string;
          transactions: string;
          accounts: string;
          send: string;
          receive: string;
          reconciliation: string;
          transactionDetail: string;
          createAccount: string;
          executionPlan: string;
          pendingActions: string;
          trace: string;
          integrity: string;
          risk: string;
          blocked: string;
          mismatch: string;
          sourceAddress: string;
          targetAddress: string;
          memo: string;
          networkTxHash: string;
          signerHint: string;
          operatorHint: string;
          address: string;
          confirmations: string;
          maxItems: string;
          strategy: string;
          reason: string;
          action: string;
          sign: string;
          broadcast: string;
          confirm: string;
          remediate: string;
          fail: string;
          batchAdvance: string;
          batchRemediate: string;
          batchReconcile: string;
        };
        vpn: {
          title: string;
          country: string;
          city: string;
          tier: string;
          load: string;
          session: string;
          sessions: string;
          servers: string;
          profiles: string;
          settings: string;
          name: string;
          protocol: string;
          routingMode: string;
          targetServer: string;
          targetCountry: string;
          secureCore: string;
          netshield: string;
          killSwitch: string;
          defaultMode: string;
          connect: string;
          history: string;
          createProfile: string;
          updateProfile: string;
          quickConnect: string;
          disconnect: string;
        };
        meet: {
          title: string;
          topic: string;
          accessLevel: string;
          startedAt: string;
          plan: string;
          current: string;
          create: string;
          maxParticipants: string;
          access: string;
          rooms: string;
          room: string;
          lobby: string;
          join: string;
          host: string;
          waitlist: string;
          activate: string;
          contactSales: string;
          company: string;
          seats: string;
          note: string;
          history: string;
          participants: string;
          guestRequests: string;
          signals: string;
          quality: string;
          displayName: string;
          joinCode: string;
          requestToken: string;
          guestSession: string;
          audio: string;
          video: string;
          screen: string;
          approve: string;
          reject: string;
          rotateJoinCode: string;
          endRoom: string;
          heartbeat: string;
          payload: string;
          jitter: string;
          packetLoss: string;
          roundTrip: string;
        };
        authenticator: {
          title: string;
          issuer: string;
          account: string;
          algorithm: string;
          secret: string;
          create: string;
          generate: string;
          sync: string;
          import: string;
          backup: string;
          settings: string;
          pin: string;
          qrImage: string;
          exported: string;
          lockTimeout: string;
        };
        community: {
          title: string;
          posts: string;
          newPost: string;
          topic: string;
          postTitle: string;
          tags: string;
          body: string;
          publish: string;
          empty: string;
          comment: string;
          send: string;
          report: string;
          bookmark: string;
          like: string;
        };
        simpleLogin: {
          title: string;
          aliases: string;
          enabled: string;
          domains: string;
          domain: string;
          defaultMailbox: string;
          defaultMailboxId: string;
          customDomainId: string;
          subdomainMode: string;
          catchAll: string;
          createPolicy: string;
        };
        standardNotes: {
          title: string;
          folders: string;
          total: string;
          checklist: string;
          exported: string;
          editor: string;
          noteTitle: string;
          type: string;
          folder: string;
          tags: string;
          create: string;
          toggleChecklist: string;
          export: string;
        };
        mailFilters: {
          title: string;
          name: string;
          targetFolder: string;
          labels: string;
          markRead: string;
          create: string;
          preview: string;
        };
        driveVersions: {
          title: string;
          version: string;
          author: string;
          compare: string;
          checksum: string;
          createdAt: string;
          restoreConfirm: string;
          restore: string;
          e2eeReady: string;
        };
        billing: {
          title: string;
          plan: string;
          subscriptions: string;
          invoices: string;
          paymentMethods: string;
          offers: string;
          invoice: string;
          offer: string;
          quote: string;
          checkoutDraft: string;
          billingCycle: string;
          seatCount: string;
          defaultPayment: string;
          total: string;
          runAction: string;
          addPaymentMethod: string;
          createQuote: string;
          createDraft: string;
          externalBillingStatus: string;
        };
      };
      form: {
        required: string;
        userName: FormMsg;
        phone: FormMsg;
        pwd: FormMsg;
        confirmPwd: FormMsg;
        code: FormMsg;
        email: FormMsg;
      };
      dropdown: Record<Global.DropdownKey, string>;
      icon: {
        themeConfig: string;
        themeSchema: string;
        lang: string;
        fullscreen: string;
        fullscreenExit: string;
        reload: string;
        collapse: string;
        expand: string;
        pin: string;
        unpin: string;
      };
      datatable: {
        itemCount: string;
        fixed: {
          left: string;
          right: string;
          unFixed: string;
        };
      };
    };

    type GetI18nKey<T extends Record<string, unknown>, K extends keyof T = keyof T> = K extends string
      ? T[K] extends Record<string, unknown>
        ? `${K}.${GetI18nKey<T[K]>}`
        : K
      : never;

    type I18nKey = GetI18nKey<Schema>;

    type TranslateOptions<Locales extends string> = import('vue-i18n').TranslateOptions<Locales>;

    interface $T {
      (key: I18nKey): string;
      (key: I18nKey, plural: number, options?: TranslateOptions<LangType>): string;
      (key: I18nKey, defaultMsg: string, options?: TranslateOptions<I18nKey>): string;
      (key: I18nKey, list: unknown[], options?: TranslateOptions<I18nKey>): string;
      (key: I18nKey, list: unknown[], plural: number): string;
      (key: I18nKey, list: unknown[], defaultMsg: string): string;
      (key: I18nKey, named: Record<string, unknown>, options?: TranslateOptions<LangType>): string;
      (key: I18nKey, named: Record<string, unknown>, plural: number): string;
      (key: I18nKey, named: Record<string, unknown>, defaultMsg: string): string;
    }
  }

  /** Service namespace */
  namespace Service {
    /** Other baseURL key */
    type OtherBaseURLKey = 'demo';

    interface ServiceConfigItem {
      /** The backend service base url */
      baseURL: string;
      /** The proxy pattern of the backend service base url */
      proxyPattern: string;
    }

    interface OtherServiceConfigItem extends ServiceConfigItem {
      key: OtherBaseURLKey;
    }

    /** The backend service config */
    interface ServiceConfig extends ServiceConfigItem {
      /** Other backend service config */
      other: OtherServiceConfigItem[];
    }

    interface SimpleServiceConfig extends Pick<ServiceConfigItem, 'baseURL'> {
      other: Record<OtherBaseURLKey, string>;
    }

    /** The backend service response data */
    type Response<T = unknown> = {
      /** The backend service response code */
      code: string | number;
      /** The backend service response message */
      msg?: string;
      message?: string;
      /** The backend trace id */
      traceId?: string;
      /** The backend request id */
      requestId?: string;
      /** The backend service response data */
      data: T;
    };

    /** The demo backend service response data */
    type DemoResponse<T = unknown> = {
      /** The backend service response code */
      status: string;
      /** The backend service response message */
      message: string;
      /** The backend service response data */
      result: T;
    };
  }
}
