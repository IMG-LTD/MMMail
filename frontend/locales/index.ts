import type { SupportedLocale } from '../constants/i18n'
import en from './en'
import zhCN from './zh-CN'
import zhTW from './zh-TW'
import { calendarEn, calendarZhCN, calendarZhTW } from './calendar'
import { authenticatorEn, authenticatorZhCN, authenticatorZhTW } from './authenticator'
import { docsEn, docsZhCN, docsZhTW } from './docs'
import {
  contactsWorkspaceEn,
  contactsWorkspaceZhCN,
  contactsWorkspaceZhTW
} from './contacts-workspace'
import {
  driveCollaborationEn,
  driveCollaborationZhCN,
  driveCollaborationZhTW
} from './drive-collaboration'
import {
  externalAccountEn,
  externalAccountZhCN,
  externalAccountZhTW
} from './external-account'
import {
  mailEasySwitchEn,
  mailEasySwitchZhCN,
  mailEasySwitchZhTW
} from './mail-easy-switch'
import {
  mailFiltersEn,
  mailFiltersZhCN,
  mailFiltersZhTW
} from './mail-filters'
import {
  mailListEn,
  mailListZhCN,
  mailListZhTW
} from './mail-list'
import {
  mailWorkspaceEn,
  mailWorkspaceZhCN,
  mailWorkspaceZhTW
} from './mail-workspace'
import {
  mailComposeEn,
  mailComposeZhCN,
  mailComposeZhTW
} from './mail-compose'
import {
  organizationsEn,
  organizationsZhCN,
  organizationsZhTW
} from './organizations'
import {
  organizationsAuthSecurityEn,
  organizationsAuthSecurityZhCN,
  organizationsAuthSecurityZhTW
} from './organizations-auth-security'
import {
  organizationsMonitorEn,
  organizationsMonitorZhCN,
  organizationsMonitorZhTW
} from './organizations-monitor'
import { passBusinessEn, passBusinessZhCN, passBusinessZhTW } from './pass-business'
import { passMonitorEn, passMonitorZhCN, passMonitorZhTW } from './pass-monitor'
import {
  passWorkspaceEn,
  passWorkspaceZhCN,
  passWorkspaceZhTW
} from './pass-workspace'
import { orgAccessEn, orgAccessZhCN, orgAccessZhTW } from './org-access'
import {
  mailFoldersEn,
  mailFoldersZhCN,
  mailFoldersZhTW
} from './mail-folders'
import { shellEn, shellZhCN, shellZhTW } from './shell'
import { sheetsSharingEn, sheetsSharingZhCN, sheetsSharingZhTW } from './sheets-sharing'
import { vpnEn, vpnZhCN, vpnZhTW } from './vpn'
import { meetEn, meetZhCN, meetZhTW } from './meet'
import {
  meetWorkspaceEn,
  meetWorkspaceZhCN,
  meetWorkspaceZhTW
} from './meet-workspace'
import {
  meetWorkspaceMessagesEn,
  meetWorkspaceMessagesZhCN,
  meetWorkspaceMessagesZhTW
} from './meet-workspace-messages'
import { walletEn, walletZhCN, walletZhTW } from './wallet'
import {
  walletWorkspaceEn,
  walletWorkspaceZhCN,
  walletWorkspaceZhTW
} from './wallet-workspace'
import {
  walletWorkspaceMessagesEn,
  walletWorkspaceMessagesZhCN,
  walletWorkspaceMessagesZhTW
} from './wallet-workspace-messages'
import { businessEn, businessZhCN, businessZhTW } from './business'
import { commandCenterEn, commandCenterZhCN, commandCenterZhTW } from './command-center'
import {
  collaborationShellEn,
  collaborationShellZhCN,
  collaborationShellZhTW
} from './collaboration-shell'
import { simpleLoginEn, simpleLoginZhCN, simpleLoginZhTW } from './simplelogin'
import {
  standardNotesEn,
  standardNotesZhCN,
  standardNotesZhTW
} from './standard-notes'
import { searchEn, searchZhCN, searchZhTW } from './search'
import {
  notificationsEn,
  notificationsZhCN,
  notificationsZhTW
} from './notifications'
import { lumoEn, lumoZhCN, lumoZhTW } from './lumo'
import { suiteBillingEn, suiteBillingZhCN, suiteBillingZhTW } from './suite-billing'
import {
  suiteBillingCenterEn,
  suiteBillingCenterZhCN,
  suiteBillingCenterZhTW
} from './suite-billing-center'
import { suitePlansEn, suitePlansZhCN, suitePlansZhTW } from './suite-plans'
import {
  communityReleaseEn,
  communityReleaseZhCN,
  communityReleaseZhTW
} from './community-release'
import {
  systemHealthEn,
  systemHealthZhCN,
  systemHealthZhTW
} from './system-health'
import { mailE2eeEn, mailE2eeZhCN, mailE2eeZhTW } from './mail-e2ee'
import { adoptionEn, adoptionZhCN, adoptionZhTW } from './adoption'
import { pwaEn, pwaZhCN, pwaZhTW } from './pwa'

export type TranslationMessages = Record<string, string>
export type TranslationCatalog = Record<SupportedLocale, TranslationMessages>

export const messages: TranslationCatalog = {
  en: {
    ...en,
    ...authenticatorEn,
    ...adoptionEn,
    ...businessEn,
    ...calendarEn,
    ...communityReleaseEn,
    ...collaborationShellEn,
    ...commandCenterEn,
    ...contactsWorkspaceEn,
    ...docsEn,
    ...driveCollaborationEn,
    ...externalAccountEn,
    ...mailEasySwitchEn,
    ...mailFiltersEn,
    ...mailFoldersEn,
    ...mailComposeEn,
    ...mailListEn,
    ...mailWorkspaceEn,
    ...meetEn,
    ...meetWorkspaceEn,
    ...meetWorkspaceMessagesEn,
    ...organizationsEn,
    ...organizationsAuthSecurityEn,
    ...organizationsMonitorEn,
    ...orgAccessEn,
    ...passBusinessEn,
    ...passMonitorEn,
    ...passWorkspaceEn,
    ...mailE2eeEn,
    ...pwaEn,
    ...sheetsSharingEn,
    ...shellEn,
    ...searchEn,
    ...simpleLoginEn,
    ...standardNotesEn,
    ...systemHealthEn,
    ...notificationsEn,
    ...lumoEn,
    ...suiteBillingEn,
    ...suiteBillingCenterEn,
    ...suitePlansEn,
    ...vpnEn,
    ...walletEn,
    ...walletWorkspaceEn,
    ...walletWorkspaceMessagesEn
  },
  'zh-CN': {
    ...zhCN,
    ...authenticatorZhCN,
    ...adoptionZhCN,
    ...businessZhCN,
    ...calendarZhCN,
    ...communityReleaseZhCN,
    ...collaborationShellZhCN,
    ...commandCenterZhCN,
    ...contactsWorkspaceZhCN,
    ...docsZhCN,
    ...driveCollaborationZhCN,
    ...externalAccountZhCN,
    ...mailEasySwitchZhCN,
    ...mailFiltersZhCN,
    ...mailFoldersZhCN,
    ...mailComposeZhCN,
    ...mailListZhCN,
    ...mailWorkspaceZhCN,
    ...meetZhCN,
    ...meetWorkspaceZhCN,
    ...meetWorkspaceMessagesZhCN,
    ...organizationsZhCN,
    ...organizationsAuthSecurityZhCN,
    ...organizationsMonitorZhCN,
    ...orgAccessZhCN,
    ...passBusinessZhCN,
    ...passMonitorZhCN,
    ...passWorkspaceZhCN,
    ...mailE2eeZhCN,
    ...pwaZhCN,
    ...sheetsSharingZhCN,
    ...shellZhCN,
    ...searchZhCN,
    ...simpleLoginZhCN,
    ...standardNotesZhCN,
    ...systemHealthZhCN,
    ...notificationsZhCN,
    ...lumoZhCN,
    ...suiteBillingZhCN,
    ...suiteBillingCenterZhCN,
    ...suitePlansZhCN,
    ...vpnZhCN,
    ...walletZhCN,
    ...walletWorkspaceZhCN,
    ...walletWorkspaceMessagesZhCN
  },
  'zh-TW': {
    ...zhTW,
    ...authenticatorZhTW,
    ...adoptionZhTW,
    ...businessZhTW,
    ...calendarZhTW,
    ...communityReleaseZhTW,
    ...collaborationShellZhTW,
    ...commandCenterZhTW,
    ...contactsWorkspaceZhTW,
    ...docsZhTW,
    ...driveCollaborationZhTW,
    ...externalAccountZhTW,
    ...mailEasySwitchZhTW,
    ...mailFiltersZhTW,
    ...mailFoldersZhTW,
    ...mailComposeZhTW,
    ...mailListZhTW,
    ...mailWorkspaceZhTW,
    ...meetZhTW,
    ...meetWorkspaceZhTW,
    ...meetWorkspaceMessagesZhTW,
    ...organizationsZhTW,
    ...organizationsAuthSecurityZhTW,
    ...organizationsMonitorZhTW,
    ...orgAccessZhTW,
    ...passBusinessZhTW,
    ...passMonitorZhTW,
    ...passWorkspaceZhTW,
    ...mailE2eeZhTW,
    ...pwaZhTW,
    ...sheetsSharingZhTW,
    ...shellZhTW,
    ...searchZhTW,
    ...simpleLoginZhTW,
    ...standardNotesZhTW,
    ...systemHealthZhTW,
    ...notificationsZhTW,
    ...lumoZhTW,
    ...suiteBillingZhTW,
    ...suiteBillingCenterZhTW,
    ...suitePlansZhTW,
    ...vpnZhTW,
    ...walletZhTW,
    ...walletWorkspaceZhTW,
    ...walletWorkspaceMessagesZhTW
  }
}
