<script setup lang="ts">
import HostedBadge from "@/design-system/components/HostedBadge.vue";
import PremiumBadge from "@/design-system/components/PremiumBadge.vue";
import MaturityBadge from "@/shared/components/MaturityBadge.vue";
import { lt, useLocaleText, type TextLike } from "@/locales";
import PublicSurfaceFrame from "./PublicSurfaceFrame.vue";

type ModuleLevel = "ga" | "beta" | "preview";
type ModuleEntitlement = "community" | "premium";

interface BoundaryModule {
  copy: TextLike;
  entitlement: ModuleEntitlement;
  hosted: boolean;
  key: string;
  level: ModuleLevel;
  name: TextLike;
}

const { tr } = useLocaleText();

const modules: BoundaryModule[] = [
  {
    key: "mail",
    name: lt("邮件", "郵件", "Mail"),
    level: "ga",
    entitlement: "community",
    hosted: false,
    copy: lt(
      "私密邮件、信任状态与共享投递。",
      "私密郵件、信任狀態與共享投遞。",
      "Private mail, trust states, share delivery.",
    ),
  },
  {
    key: "calendar",
    name: lt("日历", "日曆", "Calendar"),
    level: "ga",
    entitlement: "community",
    hosted: false,
    copy: lt(
      "可用性、ICS 流程与共享排期。",
      "可用性、ICS 流程與共享排程。",
      "Availability, ICS flow, shared scheduling.",
    ),
  },
  {
    key: "drive",
    name: lt("云盘", "雲端硬碟", "Drive"),
    level: "ga",
    entitlement: "community",
    hosted: true,
    copy: lt(
      "资源管理、共享中心与托管存储边界。",
      "資源管理、共享中心與代管儲存邊界。",
      "Explorer, sharing center, and hosted storage boundaries.",
    ),
  },
  {
    key: "pass",
    name: lt("密码库", "密碼庫", "Pass"),
    level: "beta",
    entitlement: "community",
    hosted: false,
    copy: lt(
      "保险库、监控与安全链接。",
      "保險庫、監控與安全連結。",
      "Vault, monitor, secure links.",
    ),
  },
  {
    key: "docs",
    name: lt("文档", "文件", "Docs"),
    level: "beta",
    entitlement: "community",
    hosted: false,
    copy: lt("轻量文档编辑界面。", "輕量文件編輯介面。", "Lightweight editor surface."),
  },
  {
    key: "sheets",
    name: lt("表格", "試算表", "Sheets"),
    level: "beta",
    entitlement: "premium",
    hosted: false,
    copy: lt(
      "结构化网格与高级洞察边界。",
      "結構化網格與進階洞察邊界。",
      "Structured grids with advanced insight boundaries.",
    ),
  },
  {
    key: "labs",
    name: lt("Labs", "Labs", "Labs"),
    level: "preview",
    entitlement: "premium",
    hosted: true,
    copy: lt(
      "统一预览外壳与托管实验能力。",
      "統一預覽外殼與代管實驗能力。",
      "Unified preview shell and hosted experimental capabilities.",
    ),
  },
];
</script>

<template>
  <PublicSurfaceFrame
    :title="
      lt('MMMail 产品边界与成熟度', 'MMMail 產品邊界與成熟度', 'MMMail product scope and maturity')
    "
    :description="
      lt(
        '公开说明 Community、Premium、Hosted 与成熟度边界，避免用户在开始流程后才发现能力限制。',
        '公開說明 Community、Premium、Hosted 與成熟度邊界，避免使用者在開始流程後才發現能力限制。',
        'Publicly states Community, Premium, Hosted, and maturity boundaries before users enter a flow.',
      )
    "
  >
    <section class="boundary-page">
      <div class="surface-grid boundary-page__grid">
        <article class="surface-card boundary-hero">
          <span class="section-label">{{ tr(lt("公开边界", "公開邊界", "Public boundary")) }}</span>
          <h2>
            {{
              tr(
                lt(
                  "Community 默认可用，Premium 与 Hosted 明确标注。",
                  "Community 預設可用，Premium 與 Hosted 明確標示。",
                  "Community is available by default; Premium and Hosted are explicit.",
                ),
              )
            }}
          </h2>
          <p>
            {{
              tr(
                lt(
                  "所有公开入口都保留在 Community 基线中。付费、托管或角色限制只作为可见边界呈现，不隐藏流程。",
                  "所有公開入口都保留在 Community 基線中。付費、代管或角色限制只作為可見邊界呈現，不隱藏流程。",
                  "All public entry points stay in the Community baseline. Paid, hosted, or role restrictions are visible boundaries, never hidden flow blockers.",
                ),
              )
            }}
          </p>
        </article>

        <article class="surface-card boundary-card">
          <span class="section-label">{{
            tr(lt("指导原则", "指導原則", "Guiding principles"))
          }}</span>
          <ul>
            <li>
              {{
                tr(
                  lt(
                    "公开访问不需要付费壳层。",
                    "公開存取不需要付費殼層。",
                    "Public access never requires a paid shell.",
                  ),
                )
              }}
            </li>
            <li>
              {{
                tr(
                  lt(
                    "托管能力必须说明自托管限制。",
                    "代管能力必須說明自託管限制。",
                    "Hosted capabilities must describe self-hosted limits.",
                  ),
                )
              }}
            </li>
            <li>
              {{
                tr(
                  lt(
                    "预览能力低于核心工作区。",
                    "預覽能力低於核心工作區。",
                    "Preview capabilities remain subordinate to core workspaces.",
                  ),
                )
              }}
            </li>
          </ul>
        </article>
      </div>

      <article class="surface-card boundary-matrix">
        <div class="boundary-matrix__header">
          <div>
            <span class="section-label">{{
              tr(lt("成熟度矩阵", "成熟度矩陣", "Maturity matrix"))
            }}</span>
            <h2>{{ tr(lt("当前交付边界", "目前交付邊界", "Current delivery envelope")) }}</h2>
          </div>
        </div>

        <div class="boundary-matrix__rows">
          <article v-for="module in modules" :key="module.key" class="boundary-row">
            <div>
              <strong>{{ tr(module.name) }}</strong>
              <p>{{ tr(module.copy) }}</p>
            </div>
            <div class="boundary-row__badges">
              <span v-if="module.entitlement === 'community'" class="community-badge"
                >Community</span
              >
              <PremiumBadge v-else compact />
              <HostedBadge v-if="module.hosted" compact />
              <MaturityBadge :level="module.level" compact />
            </div>
          </article>
        </div>
      </article>
    </section>
  </PublicSurfaceFrame>
</template>

<style scoped>
.boundary-page {
  display: grid;
  gap: 20px;
}

.boundary-page__grid {
  grid-template-columns: 1.35fr 0.65fr;
}

.boundary-hero,
.boundary-card,
.boundary-matrix {
  padding: 28px;
}

.boundary-hero h2,
.boundary-matrix h2 {
  margin: 8px 0 0;
  color: var(--mm-text-primary);
  font-size: 30px;
  line-height: 1.05;
  letter-spacing: 0;
}

.boundary-hero p,
.boundary-card ul,
.boundary-row p {
  color: var(--mm-text-secondary);
  line-height: 1.7;
}

.boundary-card ul {
  margin: 18px 0 0;
  padding-left: 18px;
}

.boundary-matrix__rows {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.boundary-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.boundary-row strong {
  color: var(--mm-text-primary);
}

.boundary-row p {
  margin: 6px 0 0;
  font-size: 13px;
}

.boundary-row__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.community-badge {
  display: inline-flex;
  align-items: center;
  min-height: 18px;
  padding: 0 7px;
  border: 1px solid color-mix(in srgb, var(--mm-success) 22%, white);
  border-radius: 999px;
  background: color-mix(in srgb, var(--mm-success) 10%, white);
  color: var(--mm-success);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0;
}

@media (max-width: 900px) {
  .boundary-page__grid,
  .boundary-row {
    grid-template-columns: 1fr;
  }

  .boundary-row__badges {
    justify-content: flex-start;
  }
}
</style>
