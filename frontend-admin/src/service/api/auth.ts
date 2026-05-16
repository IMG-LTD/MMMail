import { request } from '../request';

/**
 * Login
 *
 * @param email Email
 * @param password Password
 */
export function fetchLogin(email: string, password: string) {
  return request<Api.Auth.AuthPayload>({
    url: '/api/v2/auth/login',
    method: 'post',
    data: {
      email,
      password
    }
  });
}

/**
 * Register
 *
 * @param displayName Display name
 * @param email Email
 * @param password Password
 */
export function fetchRegister(displayName: string, email: string, password: string) {
  return request<Api.Auth.AuthPayload>({
    url: '/api/v2/auth/register',
    method: 'post',
    data: {
      displayName,
      email,
      password
    }
  });
}

/** Get user info */
export function fetchGetUserInfo() {
  return request<Api.Auth.UserInfo>({ url: '/api/v2/auth/me' });
}

/**
 * Refresh token
 *
 * @param refreshToken Refresh token
 */
export function fetchRefreshToken(refreshToken: string) {
  return request<Api.Auth.AuthPayload>({
    url: '/api/v2/auth/refresh',
    method: 'post',
    data: {
      refreshToken
    }
  });
}

/**
 * return custom backend error
 *
 * @param code error code
 * @param msg error message
 */
export function fetchCustomBackendError(code: string, msg: string) {
  return request({ url: '/auth/error', params: { code, msg } });
}
