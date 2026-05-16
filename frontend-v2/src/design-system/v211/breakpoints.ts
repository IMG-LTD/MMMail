import { computed } from "vue";
import { useWindowSize } from "@vueuse/core";

export const v211Breakpoints = {
  xs: 0,
  sm: 640,
  md: 960,
  lg: 1200,
  xl: 1440,
} as const;

export type V211Breakpoint = keyof typeof v211Breakpoints;

export function getV211Breakpoint(width: number): V211Breakpoint {
  if (width >= v211Breakpoints.xl) {
    return "xl";
  }

  if (width >= v211Breakpoints.lg) {
    return "lg";
  }

  if (width >= v211Breakpoints.md) {
    return "md";
  }

  if (width >= v211Breakpoints.sm) {
    return "sm";
  }

  return "xs";
}

export function useBreakpoint() {
  const { width } = useWindowSize();
  const current = computed(() => getV211Breakpoint(width.value));
  const isMobile = computed(() => current.value === "xs");
  const isRightPanelDrawer = computed(() => ["xs", "sm", "md"].includes(current.value));

  return {
    current,
    isMobile,
    isRightPanelDrawer,
    width,
  };
}
