import type { ElegantConstRoute, ElegantRoute, LastLevelRouteKey, RouteLayout } from '@elegant-router/types';
import type { RouteRecordRaw } from 'vue-router';
import { generatedRoutes } from '../elegant/routes';
import { layouts, views } from '../elegant/imports';
import { transformElegantRoutesToVueRoutes } from '../elegant/transform';
import { accessRouteMetaPatches } from './access-meta';
import { customRoutes } from './custom-routes';

function applyRouteMetaPatch(route: ElegantConstRoute): ElegantConstRoute {
  const patch = accessRouteMetaPatches[route.name];
  const patched: ElegantConstRoute = {
    ...route,
    meta: patch ? ({ ...route.meta, ...patch } as NonNullable<ElegantConstRoute['meta']>) : route.meta
  };

  if ('children' in route && route.children?.length) {
    return { ...patched, children: route.children.map(applyRouteMetaPatch) };
  }

  return patched;
}

function isCustomSingleLevelRoute(route: ElegantConstRoute) {
  return typeof route.component === 'string' && route.component.includes('$') && !route.children?.length;
}

function transformCustomSingleLevelRoute(route: ElegantConstRoute): RouteRecordRaw {
  const { name, path, component, children: _children, ...rest } = route;
  const [layoutKey, viewKey] = resolveSingleLevelRouteComponent(component as string);

  return {
    path,
    component: layouts[layoutKey],
    meta: { title: route.meta?.title || '' },
    children: [
      {
        name,
        path: '',
        component: views[viewKey],
        ...rest
      } as RouteRecordRaw
    ]
  };
}

function resolveSingleLevelRouteComponent(component: string) {
  const [layoutComponent, viewComponent] = component.split('$');
  const layoutKey = layoutComponent.replace('layout.', '');
  const viewKey = viewComponent.replace('view.', '');

  if (!isRouteLayout(layoutKey)) {
    throw new Error(`Layout component "${layoutKey}" not found`);
  }
  if (!isLastLevelRouteKey(viewKey)) {
    throw new Error(`View component "${viewKey}" not found`);
  }

  return [layoutKey, viewKey] as const;
}

function isRouteLayout(key: string): key is RouteLayout {
  return key in layouts;
}

function isLastLevelRouteKey(key: string): key is LastLevelRouteKey {
  return key in views;
}

/** create routes when the auth route mode is static */
export function createStaticRoutes() {
  const constantRoutes: ElegantRoute[] = [];

  const authRoutes: ElegantRoute[] = [];
  const patchedGeneratedRoutes = generatedRoutes.map(applyRouteMetaPatch) as ElegantRoute[];
  const routeSource = [...customRoutes, ...patchedGeneratedRoutes] as ElegantRoute[];

  routeSource.forEach(item => {
    if (item.meta?.constant) {
      constantRoutes.push(item);
    } else {
      authRoutes.push(item);
    }
  });

  return {
    constantRoutes,
    authRoutes
  };
}

/**
 * Get auth vue routes
 *
 * @param routes Elegant routes
 */
export function getAuthVueRoutes(routes: ElegantConstRoute[]) {
  return routes.flatMap(route => {
    if (isCustomSingleLevelRoute(route)) {
      return [transformCustomSingleLevelRoute(route)];
    }

    return transformElegantRoutesToVueRoutes([route], layouts, views);
  });
}
