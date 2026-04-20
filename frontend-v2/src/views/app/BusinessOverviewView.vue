<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, type TextLike, useLocaleText } from '@/locales'

const { tr } = useLocaleText()

interface MetricCard {
  id: string
  label: TextLike
  value: string
  meta: TextLike
  emphasis: boolean
  progress: number
}

interface QuickAction {
  id: string
  label: TextLike
}

interface ReviewRow {
  owner: TextLike
  recipient: TextLike
  severity: 'neutral' | 'critical'
  severityLabel: TextLike
  time: string
}

const metrics: MetricCard[] = [
  { id: 'mail-exchange', label: lt('邮件往来', '郵件往來', 'Mail exchange'), value: '24', meta: '+2', emphasis: false, progress: 0 },
  { id: 'member-count', label: lt('成员总数', '成員總數', 'Total members'), value: '1.2k', meta: lt('活跃中', '活躍中', 'Active'), emphasis: false, progress: 0 },
  { id: 'quality-score', label: lt('平均质量分', '平均品質分', 'Average quality score'), value: '42.2', meta: '/ 100TB', emphasis: false, progress: 0.42 },
  { id: 'storage-health', label: lt('存储健康分', '儲存健康分', 'Storage health score'), value: '98%', meta: '+9', emphasis: true, progress: 0.98 }
]

const quickActions: QuickAction[] = [
  { id: 'team-report', label: lt('创建团队汇报', '建立團隊報告', 'Create team report') },
  { id: 'invite-member', label: lt('邀请成员', '邀請成員', 'Invite member') },
  { id: 'open-audit', label: lt('打开审计', '開啟稽核', 'Open audit') },
  { id: 'bulk-backup', label: lt('批量备份', '批次備份', 'Bulk backup') }
]

const reviews: ReviewRow[] = [
  {
    owner: lt('多人投票', '多人投票', 'Multi-approver vote'),
    recipient: lt('已分发给 frida.li@hangzhouenergy.com', '已分發給 frida.li@hangzhouenergy.com', 'Distributed to frida.li@hangzhouenergy.com'),
    severity: 'neutral',
    severityLabel: lt('已分发', '已分發', 'Distributed'),
    time: '2023-10-24 14:32:01'
  },
  {
    owner: lt('多人投票', '多人投票', 'Multi-approver vote'),
    recipient: lt('已分发给 q4.sales@phoenix-secure.com', '已分發給 q4.sales@phoenix-secure.com', 'Distributed to q4.sales@phoenix-secure.com'),
    severity: 'neutral',
    severityLabel: lt('已分发', '已分發', 'Distributed'),
    time: '2023-10-24 14:15:45'
  },
  {
    owner: lt('多人协作', '多人協作', 'Multi-team collaboration'),
    recipient: lt('项目主负责人组需要立即签收', '專案主要負責人群組需要立即簽收', 'Primary project owners require immediate acknowledgment'),
    severity: 'critical',
    severityLabel: lt('P1 / 紧急', 'P1 / 緊急', 'P1 / Critical'),
    time: '2023-10-25 09:06:12'
  },
  {
    owner: lt('多人协作', '多人協作', 'Multi-team collaboration'),
    recipient: lt('运营侧签收在 6 小时内完成', '營運側簽收需在 6 小時內完成', 'Operations acknowledgment is due within 6 hours'),
    severity: 'neutral',
    severityLabel: lt('低风险', '低風險', 'Low risk'),
    time: '2023-10-22 16:45:08'
  }
]
</script>

<template>
  <section class="page-shell surface-grid business-page">
    <compact-page-header
      :eyebrow="lt('业务', '業務', 'Business')"
      :title="lt('业务概览', '業務概覽', 'Business overview')"
      :description="lt('2023 年第四季度发展总览与近期运营重点。', '2023 年第四季發展總覽與近期營運重點。', 'Q4 2023 growth overview and near-term operational priorities.')"
    >
      <button class="business-page__action" type="button">{{ tr(lt('生成报告', '產生報告', 'Generate report')) }}</button>
    </compact-page-header>

    <div class="business-layout">
      <div class="business-main">
        <div class="business-metrics">
          <article
            v-for="metric in metrics"
            :key="metric.id"
            class="surface-card metric-card"
            :class="{ 'metric-card--emphasis': metric.emphasis }"
          >
            <span class="section-label">{{ tr(metric.label) }}</span>
            <div class="metric-card__value">
              <strong>{{ metric.value }}</strong>
              <span>{{ tr(metric.meta) }}</span>
            </div>
            <span v-if="metric.progress" class="metric-card__progress">
              <span :style="{ width: `${metric.progress * 100}%` }" />
            </span>
          </article>
        </div>

        <article class="surface-card business-table">
          <div class="business-table__toolbar">
            <div>
              <span class="section-label">{{ tr(lt('近期代理活动', '近期代理活動', 'Recent agent activity')) }}</span>
              <p class="page-subtitle">{{ tr(lt('合规上下线引擎正在关注 4 个跨组织协作动作。', '合規上下線引擎正在關注 4 個跨組織協作動作。', 'The compliance workflow engine is tracking 4 cross-organization collaboration actions.')) }}</p>
            </div>
            <span class="business-table__hint">{{ tr(lt('查看全部 11 条', '查看全部 11 筆', 'View all 11 items')) }}</span>
          </div>

          <div class="business-table__head">
            <span>{{ tr(lt('时间', '時間', 'Time')) }}</span>
            <span>{{ tr(lt('操作人', '操作者', 'Actor')) }}</span>
            <span>{{ tr(lt('引导类型', '引導類型', 'Flow type')) }}</span>
            <span>{{ tr(lt('目标摘要', '目標摘要', 'Target summary')) }}</span>
          </div>

          <div v-for="review in reviews" :key="review.time" class="business-table__row">
            <span>{{ review.time }}</span>
            <span>{{ tr(review.owner) }}</span>
            <span class="business-table__pill" :class="`business-table__pill--${review.severity}`">
              {{ tr(review.severityLabel) }}
            </span>
            <strong>{{ tr(review.recipient) }}</strong>
          </div>
        </article>
      </div>

      <div class="business-side">
        <article class="surface-card business-actions">
          <span class="section-label">{{ tr(lt('快速操作', '快速操作', 'Quick actions')) }}</span>
          <div class="business-actions__grid">
            <button v-for="action in quickActions" :key="action.id" type="button">
              {{ tr(action.label) }}
            </button>
          </div>
        </article>

        <article class="surface-card business-note">
          <span class="section-label">{{ tr(lt('高优先级提示', '高優先級提示', 'High-priority note')) }}</span>
          <strong>{{ tr(lt('系统仍在跟踪 1 个高风险流程。', '系統仍在追蹤 1 個高風險流程。', 'The system is still tracking 1 high-risk flow.')) }}</strong>
          <p>{{ tr(lt('高风险仍未完全闭环，最近一次告警由系统于 5 分钟前生成。当前升级路径已经过 S3 审批。', '高風險仍未完全閉環，最近一次告警由系統於 5 分鐘前產生。目前升級路徑已通過 S3 批准。', 'The high-risk item is still open. The most recent warning was generated 5 minutes ago, and the current escalation path has already passed S3 approval.')) }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.business-page__action {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.business-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) 320px;
  gap: 16px;
}

.business-main,
.business-side {
  display: grid;
  gap: 16px;
}

.business-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.metric-card__value {
  display: flex;
  align-items: end;
  gap: 8px;
}

.metric-card__value strong {
  font-size: 34px;
  line-height: 0.95;
  letter-spacing: -0.05em;
  color: var(--mm-ink);
}

.metric-card__value span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.metric-card__progress {
  display: flex;
  height: 6px;
  border-radius: 999px;
  background: var(--mm-card-muted);
  overflow: hidden;
}

.metric-card__progress span {
  border-radius: inherit;
  background: linear-gradient(90deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
}

.metric-card--emphasis {
  background: linear-gradient(180deg, #132039 0%, #101827 100%);
  border-color: transparent;
  box-shadow: none;
}

.metric-card--emphasis .section-label,
.metric-card--emphasis .metric-card__value span {
  color: rgba(255, 255, 255, 0.64);
}

.metric-card--emphasis .metric-card__value strong {
  color: #fff;
}

.business-table {
  padding: 18px 20px;
}

.business-table__toolbar,
.business-table__head,
.business-table__row {
  display: grid;
  grid-template-columns: 1.2fr 0.75fr 0.9fr 1.45fr;
  gap: 16px;
}

.business-table__toolbar {
  align-items: end;
  grid-template-columns: minmax(0, 1fr) auto;
  margin-bottom: 18px;
}

.business-table__hint {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.business-table__head {
  padding: 10px 0;
  border-bottom: 1px solid var(--mm-border);
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.business-table__row {
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.business-table__row span,
.business-table__row strong {
  font-size: 13px;
}

.business-table__row span:first-child,
.business-table__row span:nth-child(2) {
  color: var(--mm-text-secondary);
}

.business-table__row strong {
  line-height: 1.5;
}

.business-table__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--mm-border);
  background: var(--mm-card-muted);
}

.business-table__pill--critical {
  border-color: rgba(216, 78, 78, 0.28);
  background: rgba(216, 78, 78, 0.1);
  color: #d84e4e;
}

.business-actions,
.business-note {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.business-actions__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.business-actions__grid button {
  min-height: 74px;
  padding: 0 14px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: var(--mm-card);
  text-align: left;
}

.business-note {
  background: linear-gradient(180deg, #15243b 0%, #101827 100%);
  border-color: transparent;
  box-shadow: none;
}

.business-note .section-label,
.business-note p {
  color: rgba(255, 255, 255, 0.66);
}

.business-note strong {
  color: #fff;
  font-size: 18px;
  line-height: 1.35;
}

.business-note p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 1120px) {
  .business-layout {
    grid-template-columns: 1fr;
  }

  .business-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .business-metrics,
  .business-table__head,
  .business-table__row,
  .business-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .business-table__toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .business-metrics,
  .business-side,
  .business-table__head,
  .business-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
