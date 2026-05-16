import VueDevtools from 'vite-plugin-vue-devtools';

const DEVTOOLS_DISABLED = 'N';

export function setupDevtoolsPlugin(viteEnv: Env.ImportMeta) {
  const { VITE_DEVTOOLS_ENABLED, VITE_DEVTOOLS_LAUNCH_EDITOR } = viteEnv;

  if (VITE_DEVTOOLS_ENABLED === DEVTOOLS_DISABLED) {
    return [];
  }

  return VueDevtools({
    launchEditor: VITE_DEVTOOLS_LAUNCH_EDITOR
  });
}
