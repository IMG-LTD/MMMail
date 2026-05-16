import type { ECharts, EChartsCoreOption } from "echarts/core";

let runtimePromise: Promise<typeof import("./runtime")> | null = null;

export type { ECharts, EChartsCoreOption };

function loadV211Runtime() {
  runtimePromise ??= import("./runtime");
  return runtimePromise;
}

export async function createV211Chart(element: HTMLElement): Promise<ECharts> {
  const runtime = await loadV211Runtime();
  return runtime.createRegisteredV211Chart(element);
}
