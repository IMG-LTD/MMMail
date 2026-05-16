import { h } from 'vue';
import { NButton } from 'naive-ui';
import { $t } from '@/locales';
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { fetchRefreshToken } from '../api';
import type { RequestInstanceState } from './type';

const ERROR_STACK_CLEAR_DELAY = 5000;

export interface RequestErrorDetail {
  readonly message: string;
  readonly code?: string;
  readonly traceId?: string;
  readonly method?: string;
  readonly url?: string;
}

export function getAuthorization() {
  const token = localStg.get('token');
  const Authorization = token ? `Bearer ${token}` : null;

  return Authorization;
}

/** refresh token */
async function handleRefreshToken() {
  const { resetStore } = useAuthStore();

  const rToken = localStg.get('refreshToken') || '';
  const { error, data } = await fetchRefreshToken(rToken);
  if (!error) {
    localStg.set('token', data.accessToken);
    localStg.set('refreshToken', data.refreshToken);
    return true;
  }

  resetStore();

  return false;
}

export async function handleExpiredRequest(state: RequestInstanceState) {
  if (!state.refreshTokenPromise) {
    state.refreshTokenPromise = handleRefreshToken();
  }

  const success = await state.refreshTokenPromise;

  setTimeout(() => {
    state.refreshTokenPromise = null;
  }, 1000);

  return success;
}

export function showErrorMsg(state: RequestInstanceState, error: string | RequestErrorDetail) {
  const detail = normalizeErrorDetail(error);
  const stackKey = createErrorStackKey(detail);

  if (!state.errMsgStack?.length) {
    state.errMsgStack = [];
  }

  const isExist = state.errMsgStack.includes(stackKey);
  if (isExist) return;

  state.errMsgStack.push(stackKey);

  if (detail.traceId) {
    showTraceErrorNotification(state, detail, stackKey);
    return;
  }

  messageProvider().error(detail.message, {
    onLeave: () => clearErrorStack(state, stackKey)
  });
}

function normalizeErrorDetail(error: string | RequestErrorDetail): RequestErrorDetail {
  return typeof error === 'string' ? { message: error } : error;
}

function createErrorStackKey(error: RequestErrorDetail) {
  return [error.message, error.code, error.traceId].filter(Boolean).join('|');
}

function showTraceErrorNotification(state: RequestInstanceState, detail: RequestErrorDetail, stackKey: string) {
  const notification = notificationProvider();

  notification.error({
    title: detail.message,
    content: `${$t('request.traceId')}: ${detail.traceId}`,
    action: () =>
      h(
        NButton,
        { size: 'small', type: 'primary', secondary: true, onClick: () => copyErrorDetails(detail) },
        { default: () => $t('request.copyDetails') }
      ),
    onAfterLeave: () => clearErrorStack(state, stackKey)
  });
}

async function copyErrorDetails(detail: RequestErrorDetail) {
  try {
    await navigator.clipboard.writeText(JSON.stringify(detail, null, 2));
    messageProvider().success($t('request.copyDetailsSuccess'));
  } catch (error) {
    messageProvider().error($t('request.copyDetailsFailed'));
    throw error;
  }
}

function clearErrorStack(state: RequestInstanceState, stackKey: string) {
  state.errMsgStack = state.errMsgStack.filter(msg => msg !== stackKey);

  setTimeout(() => {
    state.errMsgStack = [];
  }, ERROR_STACK_CLEAR_DELAY);
}

function messageProvider() {
  if (!window.$message) {
    throw new Error('Naive UI message provider is not registered');
  }

  return window.$message;
}

function notificationProvider() {
  if (!window.$notification) {
    throw new Error('Naive UI notification provider is not registered');
  }

  return window.$notification;
}
