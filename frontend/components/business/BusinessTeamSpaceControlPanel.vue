<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgMember } from '~/types/api'
import type {
  OrgTeamSpace,
  OrgTeamSpaceAccessRole,
  OrgTeamSpaceActivity,
  OrgTeamSpaceActivityCategory,
  OrgTeamSpaceMember
} from '~/types/business'
import { formatBusinessActivityType, formatBusinessTime } from '~/utils/business'

const props = defineProps<{
  activeTeamSpace: OrgTeamSpace | null
  currentAccessRole: OrgTeamSpaceAccessRole | null
  canManageCurrentSpace: boolean
  candidateOrgMembers: OrgMember[]
  teamSpaceMembers: OrgTeamSpaceMember[]
  loadingMembers: boolean
  memberMutationId: string
  addMemberUserEmail: string
  addMemberRole: OrgTeamSpaceAccessRole
  activities: OrgTeamSpaceActivity[]
  activityCategory: OrgTeamSpaceActivityCategory | ''
  loadingActivity: boolean
  readOnlyReason: string
}>()

const emit = defineEmits<{
  'update:add-member-user-email': [value: string]
  'update:add-member-role': [value: OrgTeamSpaceAccessRole]
  'add-member': []
  'update-member-role': [memberId: string, role: OrgTeamSpaceAccessRole]
  'remove-member': [memberId: string, userEmail: string]
  'update:activity-category': [value: OrgTeamSpaceActivityCategory | '']
  'refresh-activity': []
  'open-trash': []
}>()
const { t } = useI18n()

const roleOptions: OrgTeamSpaceAccessRole[] = ['MANAGER', 'EDITOR', 'VIEWER']
const activityOptions: Array<OrgTeamSpaceActivityCategory | ''> = ['', 'MEMBER', 'FILE', 'VERSION', 'TRASH']

function roleTagType(role: OrgTeamSpaceAccessRole): 'danger' | 'warning' | 'info' {
  if (role === 'MANAGER') {
    return 'danger'
  }
  if (role === 'EDITOR') {
    return 'warning'
  }
  return 'info'
}

</script>

<template>
  <aside class="mm-card governance-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('business.access.title') }}</h2>
        <p class="mm-muted">{{ t('business.access.subtitle') }}</p>
      </div>
      <div class="panel-actions">
        <el-tag v-if="currentAccessRole" effect="dark">{{ currentAccessRole }}</el-tag>
        <el-button v-if="canManageCurrentSpace" type="primary" plain @click="emit('open-trash')">{{ t('business.access.openTrash') }}</el-button>
      </div>
    </div>

    <div v-if="readOnlyReason" class="read-only-banner">
      {{ readOnlyReason }}
    </div>

    <section class="panel-block">
      <div class="block-head">
        <h3>{{ t('business.access.members') }}</h3>
        <span class="mm-muted">{{ t('business.access.activeCount', { count: teamSpaceMembers.length }) }}</span>
      </div>
      <div v-if="canManageCurrentSpace" class="add-member-form">
        <el-select
          :model-value="addMemberUserEmail"
          :placeholder="t('business.access.selectOrganizationMember')"
          filterable
          clearable
          @update:model-value="emit('update:add-member-user-email', $event)"
        >
          <el-option
            v-for="member in candidateOrgMembers"
            :key="member.id"
            :label="`${member.userEmail} · ${member.role}`"
            :value="member.userEmail"
          />
        </el-select>
        <el-select :model-value="addMemberRole" @update:model-value="emit('update:add-member-role', $event)">
          <el-option v-for="role in roleOptions" :key="role" :label="role" :value="role" />
        </el-select>
        <el-button type="primary" :loading="loadingMembers" @click="emit('add-member')">{{ t('business.access.add') }}</el-button>
      </div>
      <el-empty v-if="!canManageCurrentSpace && teamSpaceMembers.length === 0" :description="t('business.access.managerRequired')" />
      <el-empty v-else-if="teamSpaceMembers.length === 0" :description="t('business.access.noMembers')" />
      <div v-else class="member-list">
        <article v-for="member in teamSpaceMembers" :key="member.id" class="member-card">
          <div>
            <div class="member-email">{{ member.userEmail }}</div>
            <div class="member-meta">{{ formatBusinessTime(member.updatedAt) }}</div>
          </div>
          <div class="member-actions">
            <el-tag :type="roleTagType(member.role)">{{ member.role }}</el-tag>
            <el-select
              v-if="canManageCurrentSpace"
              size="small"
              class="member-role-select"
              :model-value="member.role"
              :disabled="memberMutationId === member.id"
              @update:model-value="emit('update-member-role', member.id, $event)"
            >
              <el-option v-for="role in roleOptions" :key="role" :label="role" :value="role" />
            </el-select>
            <el-button
              v-if="canManageCurrentSpace"
              size="small"
              link
              type="danger"
              :loading="memberMutationId === member.id"
              @click="emit('remove-member', member.id, member.userEmail)"
            >
              {{ t('business.access.remove') }}
            </el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="panel-block">
      <div class="block-head">
        <h3>{{ t('business.access.activity') }}</h3>
        <div class="panel-actions">
          <el-select
            class="activity-filter"
            :model-value="activityCategory"
            @update:model-value="emit('update:activity-category', $event)"
          >
            <el-option v-for="item in activityOptions" :key="item || 'ALL'" :label="item || t('business.access.filterAll')" :value="item" />
          </el-select>
          <el-button :loading="loadingActivity" @click="emit('refresh-activity')">{{ t('common.actions.refresh') }}</el-button>
        </div>
      </div>
      <el-empty v-if="activities.length === 0" :description="t('business.access.noActivity')" />
      <div v-else class="activity-list">
        <article v-for="item in activities" :key="item.id" class="activity-card">
          <div class="activity-title">{{ formatBusinessActivityType(item.eventType) }}</div>
          <div class="activity-detail">{{ item.detail }}</div>
          <div class="activity-meta">{{ item.actorEmail || t('business.access.actorSystem') }} · {{ formatBusinessTime(item.createdAt) }}</div>
        </article>
      </div>
    </section>
  </aside>
</template>

<style scoped>
.governance-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-head,
.panel-actions,
.block-head,
.member-actions,
.add-member-form {
  display: flex;
  gap: 10px;
}

.panel-head,
.block-head {
  justify-content: space-between;
  align-items: flex-start;
}

.panel-actions,
.member-actions,
.add-member-form {
  align-items: center;
  flex-wrap: wrap;
}

.panel-block {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--mm-border);
  background: rgba(255, 255, 255, 0.78);
}

.read-only-banner {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(250, 173, 20, 0.12);
  color: #8a5b00;
  font-weight: 600;
}

.member-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.member-card,
.activity-card {
  padding: 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.member-card {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.member-email,
.activity-title {
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.member-meta,
.activity-meta,
.mm-muted,
.activity-detail {
  color: var(--mm-muted);
}

.member-role-select,
.activity-filter {
  min-width: 120px;
}

.activity-detail {
  margin: 8px 0;
  font-size: 13px;
  word-break: break-word;
}

@media (max-width: 768px) {
  .panel-head,
  .block-head,
  .member-card,
  .add-member-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
