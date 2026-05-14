interface AuthRouteLike {
  fullPath?: string
  meta?: Record<string, unknown>
  path: string
}

const PUBLIC_PATHS = new Set(['/login', '/register', '/boundary', '/product-access-blocked', '/404', '/500', '/offline', '/maintenance'])
const PUBLIC_PREFIXES = ['/share/', '/public/drive/shares/', '/onboarding/', '/failure-modes']

export function resolveAuthRedirect(route: AuthRouteLike, isAuthenticated: boolean) {
  if (isAuthenticated || isPublicRoute(route)) {
    return null
  }
  if (!requiresAuthentication(route)) {
    return null
  }
  return `/login?redirect=${encodeURIComponent(route.fullPath || route.path)}`
}

function isPublicRoute(route: AuthRouteLike) {
  if (route.meta?.auth === 'public') {
    return true
  }
  return PUBLIC_PATHS.has(route.path) || PUBLIC_PREFIXES.some(prefix => route.path.startsWith(prefix))
}

function requiresAuthentication(route: AuthRouteLike) {
  return route.meta?.auth === 'required' || route.meta?.layout === 'base'
}
