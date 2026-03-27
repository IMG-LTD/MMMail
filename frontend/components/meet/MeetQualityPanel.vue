<script setup lang="ts">
import { reactive } from 'vue'
import type { MeetQualitySnapshotItem, ReportMeetQualityRequest } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { formatMeetTime, meetQualityTagType } from '~/utils/meet-workspace'

interface Props {
  snapshots: MeetQualitySnapshotItem[]
  disabled: boolean
  loading: boolean
  reporting: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  report: [payload: ReportMeetQualityRequest]
}>()

const { t } = useI18n()

const form = reactive<ReportMeetQualityRequest>({
  jitterMs: 30,
  packetLossPercent: 1,
  roundTripMs: 120
})

function onReport(): void {
  emit('report', {
    jitterMs: form.jitterMs,
    packetLossPercent: form.packetLossPercent,
    roundTripMs: form.roundTripMs
  })
}
</script>

<template>
  <section class="mm-card meet-quality-card" v-loading="loading">
    <h2 class="mm-section-title">{{ t('meet.workspace.quality.title') }}</h2>
    <div class="meet-quality-grid">
      <el-input-number v-model="form.jitterMs" :min="0" :max="5000" :step="1" />
      <el-input-number v-model="form.packetLossPercent" :min="0" :max="100" :step="1" />
      <el-input-number v-model="form.roundTripMs" :min="0" :max="10000" :step="1" />
      <el-button type="primary" :disabled="disabled" :loading="reporting" @click="onReport">
        {{ t('meet.workspace.quality.report') }}
      </el-button>
    </div>
    <p class="meet-quality-hint">{{ t('meet.workspace.quality.hint') }}</p>
    <el-table :data="snapshots" style="width: 100%">
      <el-table-column prop="snapshotId" :label="t('meet.workspace.quality.columns.snapshotId')" min-width="130" />
      <el-table-column prop="participantId" :label="t('meet.workspace.quality.columns.participant')" min-width="120" />
      <el-table-column :label="t('meet.workspace.quality.jitter')" min-width="110">
        <template #default="scope">{{ scope.row.jitterMs }} ms</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.quality.packetLoss')" min-width="120">
        <template #default="scope">{{ scope.row.packetLossPercent }}%</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.quality.rtt')" min-width="100">
        <template #default="scope">{{ scope.row.roundTripMs }} ms</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.quality.columns.score')" min-width="110">
        <template #default="scope">
          <el-tag :type="meetQualityTagType(scope.row.qualityScore)">{{ scope.row.qualityScore }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.quality.columns.createdAt')" min-width="180">
        <template #default="scope">{{ formatMeetTime(scope.row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.meet-quality-card {
  padding: 16px;
}

.meet-quality-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.meet-quality-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 1200px) {
  .meet-quality-grid {
    grid-template-columns: 1fr;
  }
}
</style>
