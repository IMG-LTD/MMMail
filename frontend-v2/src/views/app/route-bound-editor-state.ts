export function hasRouteEntityChanged(previousRouteEntityId: string | null | undefined, nextRouteEntityId: string | null | undefined) {
  return String(previousRouteEntityId || '') !== String(nextRouteEntityId || '')
}

export function createRouteEntityNavigationReset(nextRouteEntityId: string | null | undefined, nextToken: string | null | undefined) {
  return {
    entityLoading: Boolean(nextToken && nextRouteEntityId),
    saveLoading: false
  }
}

export function isCurrentRouteEntity(routeEntityId: string, loadedEntityId: string | null | undefined) {
  return Boolean(routeEntityId) && Boolean(loadedEntityId) && routeEntityId === loadedEntityId
}

export function isRouteEntityEditingLocked(routeEntityId: string, loadedEntityId: string | null | undefined, entityLoading: boolean) {
  return entityLoading || !isCurrentRouteEntity(routeEntityId, loadedEntityId)
}

export function canSubmitRouteEntitySave(
  routeEntityId: string,
  loadedEntityId: string | null | undefined,
  entityLoading: boolean,
  saveLoading: boolean,
  canEdit: boolean,
  hasChanges: boolean
) {
  return !isRouteEntityEditingLocked(routeEntityId, loadedEntityId, entityLoading)
    && !saveLoading
    && canEdit
    && hasChanges
}
