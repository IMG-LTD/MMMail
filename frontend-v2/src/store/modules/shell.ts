import { computed, ref } from "vue";
import { defineStore } from "pinia";

type ContextPanelKey = "activity" | "details" | "help" | "notifications" | "risk";

export const useShellStore = defineStore("shell", () => {
  const activeContextPanel = ref<ContextPanelKey>("activity");
  const commandPaletteOpen = ref(false);
  const contextPanelOpen = ref(true);
  const mobileMorePanelOpen = ref(false);
  const notificationDrawerOpen = ref(false);
  const quickCreateOpen = ref(false);
  const sideNavCollapsed = ref(false);

  const shellStateClasses = computed(() => ({
    "shell-state--command-open": commandPaletteOpen.value,
    "shell-state--context-open": contextPanelOpen.value,
    "shell-state--mobile-more-open": mobileMorePanelOpen.value,
    "shell-state--nav-collapsed": sideNavCollapsed.value,
    "shell-state--notifications-open": notificationDrawerOpen.value,
    "shell-state--quick-create-open": quickCreateOpen.value,
  }));

  function closeCommandPalette() {
    commandPaletteOpen.value = false;
  }

  function closeContextPanel() {
    contextPanelOpen.value = false;
  }

  function closeMobileMorePanel() {
    mobileMorePanelOpen.value = false;
  }

  function closeQuickCreate() {
    quickCreateOpen.value = false;
  }

  function openCommandPalette() {
    commandPaletteOpen.value = true;
  }

  function openContextPanel(panel: ContextPanelKey = activeContextPanel.value) {
    activeContextPanel.value = panel;
    contextPanelOpen.value = true;
  }

  function openMobileMorePanel() {
    mobileMorePanelOpen.value = true;
  }

  function openQuickCreate() {
    quickCreateOpen.value = true;
  }

  function setSideNavCollapsed(value: boolean) {
    sideNavCollapsed.value = value;
  }

  function toggleContextPanel(panel?: ContextPanelKey) {
    if (panel) {
      activeContextPanel.value = panel;
    }
    contextPanelOpen.value = !contextPanelOpen.value;
  }

  function toggleNotificationDrawer() {
    notificationDrawerOpen.value = !notificationDrawerOpen.value;
    if (notificationDrawerOpen.value) {
      openContextPanel("notifications");
    }
  }

  function toggleSideNav() {
    sideNavCollapsed.value = !sideNavCollapsed.value;
  }

  return {
    activeContextPanel,
    closeCommandPalette,
    closeContextPanel,
    closeMobileMorePanel,
    closeQuickCreate,
    commandPaletteOpen,
    contextPanelOpen,
    mobileMorePanelOpen,
    notificationDrawerOpen,
    openCommandPalette,
    openContextPanel,
    openMobileMorePanel,
    openQuickCreate,
    quickCreateOpen,
    setSideNavCollapsed,
    shellStateClasses,
    sideNavCollapsed,
    toggleContextPanel,
    toggleNotificationDrawer,
    toggleSideNav,
  };
});
