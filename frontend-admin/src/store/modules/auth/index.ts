import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { defineStore } from 'pinia';
import { useLoading } from '@sa/hooks';
import { fetchGetUserInfo, fetchLogin } from '@/service/api/auth';
import { useRouterPush } from '@/hooks/common/router';
import { localStg } from '@/utils/storage';
import { SetupStoreId } from '@/enum';
import { $t } from '@/locales';
import { useOrgStore } from '../org';
import { useRouteStore } from '../route';
import { useTabStore } from '../tab';
import { clearAuthStorage, getToken } from './shared';

const LOCK_UNTIL_PATTERN = /locked until ([\d.:-]+T[\d.:-]+)/;

export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const route = useRoute();
  const authStore = useAuthStore();
  const orgStore = useOrgStore();
  const routeStore = useRouteStore();
  const tabStore = useTabStore();
  const { toLogin, redirectFromLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  const token = ref('');
  const session = ref<Api.Auth.AuthPayload | null>(null);
  const currentOrgId = computed(() => orgStore.currentOrgId);
  const entitlements = ref<string[]>([]);
  const featureFlags = ref<string[]>([]);
  const loginRisk = ref<Api.Auth.LoginRisk | null>(null);
  const loginLockMessage = ref('');

  const userInfo: Api.Auth.UserInfo = reactive({
    userId: '',
    userName: '',
    roles: [],
    buttons: []
  });

  /** is super role in static route */
  const isStaticSuper = computed(() => {
    const { VITE_AUTH_ROUTE_MODE, VITE_STATIC_SUPER_ROLE } = import.meta.env;

    return VITE_AUTH_ROUTE_MODE === 'static' && userInfo.roles.includes(VITE_STATIC_SUPER_ROLE);
  });

  /** Is login */
  const isLogin = computed(() => Boolean(token.value));

  /** Reset auth store */
  async function resetStore() {
    recordUserId();

    clearAuthStorage();
    orgStore.resetOrgScope();

    authStore.$reset();

    if (!route.meta.constant) {
      await toLogin();
    }

    tabStore.cacheTabs();
    routeStore.resetStore();
  }

  /** Record the user ID of the previous login session Used to compare with the current user ID on next login */
  function recordUserId() {
    if (!userInfo.userId) {
      return;
    }

    // Store current user ID locally for next login comparison
    localStg.set('lastLoginUserId', userInfo.userId);
  }

  /**
   * Check if current login user is different from previous login user If different, clear all tabs
   *
   * @returns {boolean} Whether to clear all tabs
   */
  function checkTabClear(): boolean {
    if (!userInfo.userId) {
      return false;
    }

    const lastLoginUserId = localStg.get('lastLoginUserId');

    // Clear all tabs if current user is different from previous user
    if (!lastLoginUserId || lastLoginUserId !== userInfo.userId) {
      localStg.remove('globalTabs');
      tabStore.clearTabs();

      localStg.remove('lastLoginUserId');
      return true;
    }

    localStg.remove('lastLoginUserId');
    return false;
  }

  /**
   * Login
   *
   * @param userName User name
   * @param password Password
   * @param [redirect=true] Whether to redirect after login. Default is `true`
   */
  async function login(userName: string, password: string, redirect = true) {
    startLoading();

    const { data: payload, error } = await fetchLogin(userName, password);

    if (!error) {
      const pass = await applyAuthPayload(payload);

      if (pass) {
        // Check if the tab needs to be cleared
        const isClear = checkTabClear();
        let needRedirect = redirect;

        if (isClear) {
          // If the tab needs to be cleared,it means we don't need to redirect.
          needRedirect = false;
        }
        await redirectFromLogin(needRedirect);

        window.$notification?.success({
          title: $t('page.login.common.loginSuccess'),
          content: $t('page.login.common.welcomeBack', { userName: userInfo.userName }),
          duration: 4500
        });
      }
    } else {
      handleLoginFailure(error);
      clearAuthStorage();
      token.value = '';
      session.value = null;
    }

    endLoading();
  }

  async function applyAuthPayload(payload: Api.Auth.AuthPayload) {
    // 1. stored in the localStorage, the later requests need it in headers
    localStg.set('token', payload.accessToken);
    localStg.set('refreshToken', payload.refreshToken);

    syncAccessState(payload);
    normalizeUserInfo(payload.user);
    token.value = payload.accessToken;
    loginLockMessage.value = '';
    notifyLoginSecurity(payload);

    return true;
  }

  function syncAccessState(payload: Api.Auth.AuthPayload) {
    session.value = payload;
    entitlements.value = [...(payload.entitlements || [])];
    featureFlags.value = [...(payload.featureFlags || [])];
    orgStore.setCurrentOrgId(payload.currentOrgId || '');
  }

  function normalizeUserInfo(profile: Api.Auth.UserInfo) {
    const roles = profile.roles?.length ? profile.roles : profile.role ? [profile.role] : [];
    Object.assign(userInfo, profile, {
      userId: String(profile.userId || profile.id || ''),
      userName: profile.userName || profile.displayName || profile.email || '',
      roles,
      buttons: profile.buttons || []
    });
  }

  function notifyLoginSecurity(payload: Api.Auth.AuthPayload) {
    loginRisk.value = payload.secondFactorRequired
      ? {
          risk: payload.risk,
          riskReasons: payload.riskReasons,
          secondFactorRequired: payload.secondFactorRequired,
          securityEventId: payload.securityEventId
        }
      : null;
    if (!payload.secondFactorRequired) {
      return;
    }
    window.$notification?.warning({
      title: $t('page.login.security.title'),
      content: $t('page.login.security.secondFactorRequired'),
      duration: 6000
    });
  }

  function handleLoginFailure(error: unknown) {
    const message = resolveLoginErrorMessage(error);
    loginRisk.value = null;
    loginLockMessage.value = buildLockMessage(message);
    if (!loginLockMessage.value) {
      return;
    }
    window.$notification?.error({
      title: $t('page.login.security.lockTitle'),
      content: loginLockMessage.value,
      duration: 6000
    });
  }

  function resolveLoginErrorMessage(error: unknown) {
    const candidate = error as { message?: string; response?: { data?: { message?: string; msg?: string } } };
    return candidate.response?.data?.message || candidate.response?.data?.msg || candidate.message || '';
  }

  function buildLockMessage(message: string) {
    const match = LOCK_UNTIL_PATTERN.exec(message);
    if (!match) {
      return '';
    }
    return $t('page.login.security.lockCountdown', { time: match[1].replace('T', ' ') });
  }

  function canAccess(meta: Api.Auth.AccessDecisionMeta = {}) {
    return hasOrg(meta) && hasFeatureFlag(meta) && hasRole(accessRoles(meta)) && hasEntitlements(meta);
  }

  function hasOrg(meta: Api.Auth.AccessDecisionMeta) {
    return !meta.orgRequired || Boolean(currentOrgId.value);
  }

  function hasFeatureFlag(meta: Api.Auth.AccessDecisionMeta) {
    return !meta.featureFlag || featureFlags.value.includes(meta.featureFlag);
  }

  function hasRole(roles: string[] = []) {
    return !roles.length || roles.some(role => userInfo.roles.includes(role));
  }

  function accessRoles(meta: Api.Auth.AccessDecisionMeta) {
    return meta.role ? [...(meta.roles || []), meta.role] : meta.roles || [];
  }

  function hasEntitlements(meta: Api.Auth.AccessDecisionMeta) {
    return hasRequiredEntitlements(meta.requires) && hasAnyEntitlement(meta.anyOf);
  }

  function hasRequiredEntitlements(requires: string[] = []) {
    return requires.every(item => entitlements.value.includes(item));
  }

  function hasAnyEntitlement(anyOf: string[] = []) {
    return !anyOf.length || anyOf.some(item => entitlements.value.includes(item));
  }

  async function getUserInfo() {
    const { data: info, error } = await fetchGetUserInfo();

    if (!error) {
      normalizeUserInfo(info);
      syncUserInfoAccessState(info);

      return true;
    }

    return false;
  }

  function syncUserInfoAccessState(info: Api.Auth.UserInfo) {
    entitlements.value = [...(info.entitlements || [])];
    featureFlags.value = [...(info.featureFlags || [])];
    orgStore.setCurrentOrgId(info.currentOrgId || '');
  }

  async function initUserInfo() {
    const maybeToken = getToken();

    if (maybeToken) {
      token.value = maybeToken;
      const pass = await getUserInfo();

      if (!pass) {
        resetStore();
      }
    }
  }

  return {
    token,
    userInfo,
    isStaticSuper,
    isLogin,
    loginLoading,
    resetStore,
    login,
    applyAuthPayload,
    currentOrgId,
    entitlements,
    featureFlags,
    loginRisk,
    loginLockMessage,
    canAccess,
    initUserInfo,
    handleLoginFailure
  };
});
