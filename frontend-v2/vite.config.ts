import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

function getEchartsChunk(id: string) {
  if (!id.includes("node_modules/echarts") && !id.includes("node_modules/zrender")) {
    return undefined;
  }

  if (id.includes("node_modules/zrender")) {
    return "echarts-zrender";
  }

  if (id.includes("/lib/chart/")) {
    return "echarts-charts";
  }

  if (id.includes("/lib/component/")) {
    return "echarts-components";
  }

  if (id.includes("/lib/coord/")) {
    return "echarts-coord";
  }

  if (id.includes("/lib/data/") || id.includes("/lib/model/")) {
    return "echarts-data-model";
  }

  if (id.includes("/lib/util/") || id.includes("/lib/visual/")) {
    return "echarts-util";
  }

  return "echarts-core";
}

export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          return getEchartsChunk(id);
        },
      },
    },
  },
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
