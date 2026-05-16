export const v211FeatureFlags = {
  shell: import.meta.env.VITE_V211_SHELL !== "false",
  debugGrid: import.meta.env.VITE_V211_DEBUG_GRID === "true",
} as const;

export function isV211ShellEnabled() {
  return v211FeatureFlags.shell;
}

export function isV211DebugGridEnabled() {
  return v211FeatureFlags.debugGrid;
}
