import type { ElegantConstRoute } from '@elegant-router/types';
import { publicShareRoutes } from './public-share-routes';

/**
 * custom routes
 *
 * @link https://github.com/soybeanjs/elegant-router?tab=readme-ov-file#custom-route
 */
export const customRoutes: ElegantConstRoute[] = [
  ...publicShareRoutes,
  {
    name: 'search',
    path: '/search',
    component: 'layout.base$view.search',
    meta: {
      title: 'search',
      i18nKey: 'route.search',
      hideInMenu: true
    }
  },
  {
    name: 'admin_billing',
    path: '/admin/billing',
    component: 'layout.base$view.admin',
    meta: {
      title: 'admin_billing',
      i18nKey: 'page.billing.title',
      hideInMenu: true,
      roles: ['BILLING_ADMIN'],
      fallback: 'forbidden'
    }
  },
  {
    name: 'admin_billing_subscriptions',
    path: '/admin/billing/subscriptions',
    component: 'layout.base$view.admin',
    meta: {
      title: 'admin_billing_subscriptions',
      i18nKey: 'page.billing.subscriptions',
      hideInMenu: true,
      roles: ['BILLING_ADMIN'],
      fallback: 'forbidden'
    }
  },
  {
    name: 'admin_billing_invoices',
    path: '/admin/billing/invoices',
    component: 'layout.base$view.admin',
    meta: {
      title: 'admin_billing_invoices',
      i18nKey: 'page.billing.invoices',
      hideInMenu: true,
      roles: ['BILLING_ADMIN'],
      fallback: 'forbidden'
    }
  },
  {
    name: 'admin_billing_payment_methods',
    path: '/admin/billing/payment-methods',
    component: 'layout.base$view.admin',
    meta: {
      title: 'admin_billing_payment_methods',
      i18nKey: 'page.billing.paymentMethods',
      hideInMenu: true,
      roles: ['BILLING_ADMIN'],
      fallback: 'forbidden'
    }
  },
  {
    name: 'admin_billing_offers',
    path: '/admin/billing/offers',
    component: 'layout.base$view.admin',
    meta: {
      title: 'admin_billing_offers',
      i18nKey: 'page.billing.offers',
      hideInMenu: true,
      roles: ['BILLING_ADMIN'],
      fallback: 'forbidden'
    }
  },
  {
    name: 'meet_access',
    path: '/meet/access',
    component: 'layout.base$view.meet',
    meta: {
      title: 'meet_access',
      i18nKey: 'page.meet.access',
      hideInMenu: true,
      premiumOnly: true,
      featureFlag: 'feat.meet.enabled',
      requires: ['MEET'],
      fallback: 'contact-sales'
    }
  },
  {
    name: 'meet_rooms',
    path: '/meet/rooms',
    component: 'layout.base$view.meet',
    meta: {
      title: 'meet_rooms',
      i18nKey: 'page.meet.rooms',
      hideInMenu: true,
      premiumOnly: true,
      featureFlag: 'feat.meet.enabled',
      requires: ['MEET'],
      fallback: 'contact-sales'
    }
  },
  {
    name: 'meet_room_detail',
    path: '/meet/rooms/:roomId',
    component: 'layout.base$view.meet',
    props: true,
    meta: {
      title: 'meet_room_detail',
      i18nKey: 'page.meet.room',
      hideInMenu: true,
      premiumOnly: true,
      featureFlag: 'feat.meet.enabled',
      requires: ['MEET'],
      fallback: 'contact-sales'
    }
  },
  {
    name: 'meet_room_lobby',
    path: '/meet/rooms/:roomId/lobby',
    component: 'layout.base$view.meet',
    props: true,
    meta: {
      title: 'meet_room_lobby',
      i18nKey: 'page.meet.lobby',
      hideInMenu: true,
      premiumOnly: true,
      featureFlag: 'feat.meet.enabled',
      requires: ['MEET'],
      fallback: 'contact-sales'
    }
  },
  {
    name: 'meet_join',
    path: '/meet/join/:joinCode',
    component: 'layout.blank$view.meet',
    props: true,
    meta: {
      title: 'meet_join',
      i18nKey: 'page.meet.join',
      constant: true,
      hideInMenu: true,
      featureFlag: 'feat.meet.enabled',
      fallback: 'contact-sales'
    }
  },
  {
    name: 'meet_host',
    path: '/meet/host/:roomId',
    component: 'layout.base$view.meet',
    props: true,
    meta: {
      title: 'meet_host',
      i18nKey: 'page.meet.host',
      hideInMenu: true,
      premiumOnly: true,
      featureFlag: 'feat.meet.enabled',
      requires: ['MEET'],
      fallback: 'contact-sales'
    }
  },
  {
    name: 'wallet_accounts',
    path: '/wallet/accounts',
    component: 'layout.base$view.wallet',
    meta: {
      title: 'wallet_accounts',
      i18nKey: 'page.wallet.accounts',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.wallet.enabled',
      requires: ['WALLET'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'wallet_transactions',
    path: '/wallet/transactions',
    component: 'layout.base$view.wallet',
    meta: {
      title: 'wallet_transactions',
      i18nKey: 'page.wallet.transactions',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.wallet.enabled',
      requires: ['WALLET'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'wallet_transaction_detail',
    path: '/wallet/transactions/:transactionId',
    component: 'layout.base$view.wallet',
    props: true,
    meta: {
      title: 'wallet_transaction_detail',
      i18nKey: 'page.wallet.transactionDetail',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.wallet.enabled',
      requires: ['WALLET'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'wallet_send',
    path: '/wallet/send',
    component: 'layout.base$view.wallet',
    meta: {
      title: 'wallet_send',
      i18nKey: 'page.wallet.send',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.wallet.enabled',
      requires: ['WALLET'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'wallet_receive',
    path: '/wallet/receive',
    component: 'layout.base$view.wallet',
    meta: {
      title: 'wallet_receive',
      i18nKey: 'page.wallet.receive',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.wallet.enabled',
      requires: ['WALLET'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'wallet_reconciliation',
    path: '/wallet/reconciliation',
    component: 'layout.base$view.wallet',
    meta: {
      title: 'wallet_reconciliation',
      i18nKey: 'page.wallet.reconciliation',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.wallet.enabled',
      requires: ['WALLET'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'mail_rules',
    path: '/mail/rules',
    component: 'layout.base$view.mail',
    meta: {
      title: 'mail_rules',
      i18nKey: 'page.mailFilters.title',
      hideInMenu: true
    }
  },
  {
    name: 'mail_rule_new',
    path: '/mail/rules/new',
    component: 'layout.base$view.mail',
    meta: {
      title: 'mail_rule_new',
      i18nKey: 'page.mailFilters.create',
      hideInMenu: true
    }
  },
  {
    name: 'mail_rule_detail',
    path: '/mail/rules/:ruleId',
    component: 'layout.base$view.mail',
    props: true,
    meta: {
      title: 'mail_rule_detail',
      i18nKey: 'page.mailFilters.title',
      hideInMenu: true
    }
  },
  {
    name: 'mail_folder',
    path: '/mail/:folder',
    component: 'layout.base$view.mail',
    props: true,
    meta: {
      title: 'mail_folder',
      i18nKey: 'route.mail',
      hideInMenu: true,
      orgRequired: true,
      requires: ['mail.read']
    }
  },
  {
    name: 'vpn_servers',
    path: '/vpn/servers',
    component: 'layout.base$view.vpn',
    meta: {
      title: 'vpn_servers',
      i18nKey: 'page.vpn.servers',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.vpn.enabled',
      requires: ['VPN'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'vpn_profiles',
    path: '/vpn/profiles',
    component: 'layout.base$view.vpn',
    meta: {
      title: 'vpn_profiles',
      i18nKey: 'page.vpn.profiles',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.vpn.enabled',
      requires: ['VPN'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'vpn_sessions',
    path: '/vpn/sessions',
    component: 'layout.base$view.vpn',
    meta: {
      title: 'vpn_sessions',
      i18nKey: 'page.vpn.sessions',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.vpn.enabled',
      requires: ['VPN'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'vpn_settings',
    path: '/vpn/settings',
    component: 'layout.base$view.vpn',
    meta: {
      title: 'vpn_settings',
      i18nKey: 'page.vpn.settings',
      hideInMenu: true,
      orgRequired: true,
      premiumOnly: true,
      featureFlag: 'feat.vpn.enabled',
      requires: ['VPN'],
      fallback: 'upgrade'
    }
  },
  {
    name: 'security_authenticator_import',
    path: '/security/authenticator/import',
    component: 'layout.base$view.security_authenticator',
    meta: {
      title: 'security_authenticator_import',
      i18nKey: 'page.authenticator.import',
      hideInMenu: true
    }
  },
  {
    name: 'security_authenticator_backup',
    path: '/security/authenticator/backup',
    component: 'layout.base$view.security_authenticator',
    meta: {
      title: 'security_authenticator_backup',
      i18nKey: 'page.authenticator.backup',
      hideInMenu: true
    }
  },
  {
    name: 'security_authenticator_settings',
    path: '/security/authenticator/settings',
    component: 'layout.base$view.security_authenticator',
    meta: {
      title: 'security_authenticator_settings',
      i18nKey: 'page.authenticator.settings',
      hideInMenu: true
    }
  },
  {
    name: 'security_authenticator_detail',
    path: '/security/authenticator/:entryId',
    component: 'layout.base$view.security_authenticator',
    props: true,
    meta: {
      title: 'security_authenticator_detail',
      i18nKey: 'route.security_authenticator',
      hideInMenu: true
    }
  },
  {
    name: 'drive_file_versions',
    path: '/drive/files/:fileId/versions',
    component: 'layout.base$view.drive',
    props: true,
    meta: {
      title: 'drive_file_versions',
      i18nKey: 'page.driveVersions.title',
      hideInMenu: true
    }
  },
  {
    name: 'drive_file_secure_share',
    path: '/drive/files/:fileId/share/secure',
    component: 'layout.base$view.drive',
    props: true,
    meta: {
      title: 'drive_file_secure_share',
      i18nKey: 'page.driveSecureShare.title',
      hideInMenu: true
    }
  },
  {
    name: 'drive_file_version_compare',
    path: '/drive/files/:fileId/versions/:verA/compare/:verB',
    component: 'layout.base$view.drive',
    props: true,
    meta: {
      title: 'drive_file_version_compare',
      i18nKey: 'page.driveVersions.title',
      hideInMenu: true
    }
  }
];
