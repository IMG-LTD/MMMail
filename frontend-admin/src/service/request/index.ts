import { BACKEND_ERROR_CODE, createFlatRequest, createRequest, type AxiosResponse } from '@sa/axios';
import { useAuthStore } from '@/store/modules/auth';
import { useOrgStore } from '@/store/modules/org';
import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { $t } from '@/locales';
import { getAuthorization, handleExpiredRequest, showErrorMsg } from './shared';
import type { RequestInstanceState } from './type';

const REQUEST_ID_RESPONSE_HEADER = 'x-request-id';

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL, otherBaseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

export const request = createFlatRequest(
  {
    baseURL,
    headers: createBaseHeaders()
  },
  {
    defaultState: {
      errMsgStack: [],
      refreshTokenPromise: null
    } as RequestInstanceState,
    transform(response: AxiosResponse<App.Service.Response<any>>) {
      return response.data.data;
    },
    async onRequest(config) {
      const Authorization = getAuthorization();
      const orgStore = useOrgStore();
      Object.assign(config.headers, {
        Authorization,
        'X-Org-Id': orgStore.currentOrgId || undefined
      });

      return config;
    },
    isBackendSuccess(response) {
      // when the backend response code is "0", it means the request is success
      // to change this logic by yourself, you can modify the `VITE_SERVICE_SUCCESS_CODE` in `.env` file
      return String(response.data.code) === import.meta.env.VITE_SERVICE_SUCCESS_CODE;
    },
    async onBackendFail(response, instance) {
      const authStore = useAuthStore();
      const responseCode = String(response.data.code);

      function handleLogout() {
        authStore.resetStore();
      }

      function logoutAndCleanup() {
        handleLogout();
        window.removeEventListener('beforeunload', handleLogout);

        request.state.errMsgStack = request.state.errMsgStack.filter(msg => msg !== backendMessage(response));
      }

      // when the backend response code is in `logoutCodes`, it means the user will be logged out and redirected to login page
      const logoutCodes = import.meta.env.VITE_SERVICE_LOGOUT_CODES?.split(',') || [];
      if (logoutCodes.includes(responseCode)) {
        handleLogout();
        return null;
      }

      // when the backend response code is in `modalLogoutCodes`, it means the user will be logged out by displaying a modal
      const modalLogoutCodes = import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES?.split(',') || [];
      if (modalLogoutCodes.includes(responseCode) && !request.state.errMsgStack?.includes(backendMessage(response))) {
        request.state.errMsgStack = [...(request.state.errMsgStack || []), backendMessage(response)];

        // prevent the user from refreshing the page
        window.addEventListener('beforeunload', handleLogout);

        window.$dialog?.error({
          title: $t('common.error'),
          content: backendMessage(response),
          positiveText: $t('common.confirm'),
          maskClosable: false,
          closeOnEsc: false,
          onPositiveClick() {
            logoutAndCleanup();
          },
          onClose() {
            logoutAndCleanup();
          }
        });

        return null;
      }

      // when the backend response code is in `expiredTokenCodes`, it means the token is expired, and refresh token
      // the api `refreshToken` can not return error code in `expiredTokenCodes`, otherwise it will be a dead loop, should return `logoutCodes` or `modalLogoutCodes`
      const expiredTokenCodes = import.meta.env.VITE_SERVICE_EXPIRED_TOKEN_CODES?.split(',') || [];
      if (expiredTokenCodes.includes(responseCode)) {
        const success = await handleExpiredRequest(request.state);
        if (success) {
          const Authorization = getAuthorization();
          Object.assign(response.config.headers, { Authorization });

          return instance.request(response.config) as Promise<AxiosResponse>;
        }
      }

      return null;
    },
    onError(error) {
      // when the request is fail, you can show error message

      let message = error.message;
      let backendErrorCode = '';
      let traceId = '';

      // get backend error message and code
      if (error.code === BACKEND_ERROR_CODE) {
        message = error.response?.data?.message || error.response?.data?.msg || message;
        backendErrorCode = String(error.response?.data?.code || '');
        traceId = extractBackendTraceId(error.response);
      }

      // the error message is displayed in the modal
      const modalLogoutCodes = import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES?.split(',') || [];
      if (modalLogoutCodes.includes(backendErrorCode)) {
        return;
      }

      // when the token is expired, refresh token and retry request, so no need to show error message
      const expiredTokenCodes = import.meta.env.VITE_SERVICE_EXPIRED_TOKEN_CODES?.split(',') || [];
      if (expiredTokenCodes.includes(backendErrorCode)) {
        return;
      }

      showErrorMsg(request.state, {
        code: backendErrorCode,
        message,
        method: error.config?.method?.toUpperCase(),
        traceId,
        url: error.config?.url
      });
    }
  }
);

function createBaseHeaders() {
  const apifoxToken = import.meta.env.VITE_APIFOX_TOKEN?.trim();

  return apifoxToken ? { apifoxToken } : {};
}

function backendMessage(response: AxiosResponse<App.Service.Response<any>>) {
  return response.data.message || response.data.msg || '';
}

function extractBackendTraceId(response: AxiosResponse<App.Service.Response<any>> | undefined) {
  return response?.data?.traceId || response?.data?.requestId || responseHeader(response, REQUEST_ID_RESPONSE_HEADER);
}

function responseHeader(response: AxiosResponse<App.Service.Response<any>> | undefined, headerName: string) {
  const headers = response?.headers as ({ get?: (name: string) => unknown } & Record<string, unknown>) | undefined;
  if (!headers) return '';

  const headerValue =
    headers.get?.(headerName) ||
    headers[headerName] ||
    headers[headerName.toLowerCase()] ||
    headers[headerName.toUpperCase()];

  return normalizeHeaderValue(headerValue);
}

function normalizeHeaderValue(value: unknown): string {
  if (Array.isArray(value)) {
    return normalizeHeaderValue(value[0]);
  }

  return typeof value === 'string' ? value : '';
}

export const demoRequest = createRequest(
  {
    baseURL: otherBaseURL.demo
  },
  {
    transform(response: AxiosResponse<App.Service.DemoResponse>) {
      return response.data.result;
    },
    async onRequest(config) {
      const { headers } = config;

      // set token
      const token = localStg.get('token');
      const Authorization = token ? `Bearer ${token}` : null;
      Object.assign(headers, { Authorization });

      return config;
    },
    isBackendSuccess(response) {
      // when the backend response code is "200", it means the request is success
      // you can change this logic by yourself
      return response.data.status === '200';
    },
    async onBackendFail(_response) {
      // when the backend response code is not "200", it means the request is fail
      // for example: the token is expired, refresh token and retry request
    },
    onError(error) {
      // when the request is fail, you can show error message

      let message = error.message;

      // show backend error message
      if (error.code === BACKEND_ERROR_CODE) {
        message = error.response?.data?.message || message;
      }

      window.$message?.error(message);
    }
  }
);
