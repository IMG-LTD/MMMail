import type { ElegantConstRoute } from '@elegant-router/types';

export const publicShareRoutes: ElegantConstRoute[] = [
  {
    name: 'public_mail_share',
    path: '/share/mail/:token',
    component: 'layout.blank$view.share',
    props: true,
    meta: {
      title: 'public_mail_share',
      i18nKey: 'page.publicShare.title',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: 'public_pass_share',
    path: '/share/pass/:token',
    component: 'layout.blank$view.share',
    props: true,
    meta: {
      title: 'public_pass_share',
      i18nKey: 'page.publicShare.title',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: 'public_drive_share',
    path: '/share/drive/:token',
    component: 'layout.blank$view.share',
    props: true,
    meta: {
      title: 'public_drive_share',
      i18nKey: 'page.publicShare.title',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: 'public_share',
    path: '/share/:token',
    component: 'layout.blank$view.share',
    props: true,
    meta: {
      title: 'public_share',
      i18nKey: 'page.publicShare.title',
      constant: true,
      hideInMenu: true
    }
  }
];
