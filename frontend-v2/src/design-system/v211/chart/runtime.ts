import { init, use, type ECharts } from "echarts/core";
import { install as BarChart } from "echarts/lib/chart/bar/install.js";
import { install as GaugeChart } from "echarts/lib/chart/gauge/install.js";
import { install as LineChart } from "echarts/lib/chart/line/install.js";
import { install as PieChart } from "echarts/lib/chart/pie/install.js";
import { install as GridComponent } from "echarts/lib/component/grid/install.js";
import { install as LegendComponent } from "echarts/lib/component/legend/install.js";
import { install as TooltipComponent } from "echarts/lib/component/tooltip/install.js";
import { install as CanvasRenderer } from "echarts/lib/renderer/installCanvasRenderer.js";

let registered = false;

function registerV211Charts() {
  if (registered) {
    return;
  }

  use([
    LineChart,
    BarChart,
    PieChart,
    GaugeChart,
    GridComponent,
    TooltipComponent,
    LegendComponent,
    CanvasRenderer,
  ]);
  registered = true;
}

export function createRegisteredV211Chart(element: HTMLElement): ECharts {
  registerV211Charts();
  return init(element);
}
