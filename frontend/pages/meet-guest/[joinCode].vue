<script setup lang="ts">
import '~/assets/styles/meet-guest-join.css'

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useMeetGuestApi } from '~/composables/useMeetGuestApi'
import type { MeetGuestJoinOverview, MeetGuestRequestItem, MeetGuestSession } from '~/types/api'
import { meetGuestSessionBadgeType, resolveMeetGuestViewState } from '~/utils/meet-guest'

definePageMeta({
  layout: false,
  path: '/meet/join/:joinCode'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const {
  getMeetGuestJoinOverview,
  getMeetGuestRequest,
  getMeetGuestSession,
  heartbeatMeetGuestSession,
  leaveMeetGuestSession,
  submitMeetGuestRequest,
  updateMeetGuestSessionMedia
} = useMeetGuestApi()

const overview = ref<MeetGuestJoinOverview | null>(null)
const guestRequest = ref<MeetGuestRequestItem | null>(null)
const guestSession = ref<MeetGuestSession | null>(null)
const loadingOverview = ref(false)
const submitting = ref(false)
const pollingRequest = ref(false)
const loadingSession = ref(false)
const updatingMedia = ref(false)
const heartbeating = ref(false)
const leaving = ref(false)
const requestPollTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const sessionPollTimer = ref<ReturnType<typeof setTimeout> | null>(null)

const form = reactive({
  displayName: '',
  audioEnabled: true,
  videoEnabled: true
})

const joinCode = computed(() => String(route.params.joinCode || '').trim().toUpperCase())
const routeRequestToken = computed(() => typeof route.query.request === 'string' ? route.query.request : '')
const routeSessionToken = computed(() => typeof route.query.session === 'string' ? route.query.session : '')
const requestToken = computed(() => guestRequest.value?.requestToken || routeRequestToken.value)
const sessionToken = computed(() => guestRequest.value?.guestSessionToken || routeSessionToken.value)
const viewState = computed(() => resolveMeetGuestViewState(guestRequest.value, guestSession.value))
const sessionTagType = computed(() => meetGuestSessionBadgeType(guestSession.value?.sessionStatus))
const overviewAccessLabel = computed(() => {
  if (!overview.value) {
    return ''
  }
  return t(`meet.guest.join.accessLevel.${overview.value.accessLevel}`)
})
const sessionStatusText = computed(() => t(`meet.guest.session.status.${guestSession.value?.sessionStatus || 'WAITING'}`))
const canManageSession = computed(() => viewState.value === 'ACTIVE')

function clearRequestPollTimer(): void {
  if (!requestPollTimer.value) {
    return
  }
  clearTimeout(requestPollTimer.value)
  requestPollTimer.value = null
}

function clearSessionPollTimer(): void {
  if (!sessionPollTimer.value) {
    return
  }
  clearTimeout(sessionPollTimer.value)
  sessionPollTimer.value = null
}

async function syncRouteTokens(nextRequestToken?: string, nextSessionToken?: string): Promise<void> {
  await router.replace({
    path: route.path,
    query: {
      request: nextRequestToken || undefined,
      session: nextSessionToken || undefined
    }
  })
}

function scheduleRequestPoll(delayMs = 2500): void {
  clearRequestPollTimer()
  requestPollTimer.value = setTimeout(() => {
    requestPollTimer.value = null
    void loadGuestRequest(true)
  }, delayMs)
}

function scheduleSessionPoll(delayMs = 3000): void {
  clearSessionPollTimer()
  sessionPollTimer.value = setTimeout(() => {
    sessionPollTimer.value = null
    void loadGuestSession(true)
  }, delayMs)
}

async function loadOverview(): Promise<void> {
  loadingOverview.value = true
  try {
    overview.value = await getMeetGuestJoinOverview(joinCode.value)
  } catch (error) {
    ElMessage.error((error as Error).message || t('meet.guest.join.loadFailed'))
  } finally {
    loadingOverview.value = false
  }
}

async function loadGuestRequest(silent = false): Promise<void> {
  if (!requestToken.value) {
    return
  }
  if (!silent) {
    pollingRequest.value = true
  }
  try {
    guestRequest.value = await getMeetGuestRequest(requestToken.value)
    await syncRouteTokens(guestRequest.value.requestToken, guestRequest.value.guestSessionToken || undefined)
    if (guestRequest.value.guestSessionToken) {
      clearRequestPollTimer()
      await loadGuestSession(true)
      return
    }
    if (guestRequest.value.status === 'PENDING') {
      scheduleRequestPoll()
    } else {
      clearRequestPollTimer()
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error((error as Error).message || t('meet.guest.join.requestFailed'))
    }
  } finally {
    pollingRequest.value = false
  }
}

async function loadGuestSession(silent = false): Promise<void> {
  if (!sessionToken.value) {
    return
  }
  if (!silent) {
    loadingSession.value = true
  }
  try {
    guestSession.value = await getMeetGuestSession(sessionToken.value)
    if (guestSession.value.sessionStatus === 'ACTIVE' || guestSession.value.sessionStatus === 'WAITING') {
      scheduleSessionPoll()
    } else {
      clearSessionPollTimer()
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error((error as Error).message || t('meet.guest.join.sessionFailed'))
    }
  } finally {
    loadingSession.value = false
  }
}

async function initializeGuestFlow(): Promise<void> {
  await loadOverview()
  if (routeRequestToken.value) {
    await loadGuestRequest(true)
  }
  if (routeSessionToken.value) {
    await loadGuestSession(true)
  }
}

async function submitRequest(): Promise<void> {
  if (form.displayName.trim().length < 2) {
    ElMessage.warning(t('meet.guest.join.nameInvalid'))
    return
  }
  submitting.value = true
  try {
    guestRequest.value = await submitMeetGuestRequest(joinCode.value, {
      displayName: form.displayName.trim(),
      audioEnabled: form.audioEnabled,
      videoEnabled: form.videoEnabled
    })
    await syncRouteTokens(guestRequest.value.requestToken, guestRequest.value.guestSessionToken || undefined)
    if (guestRequest.value.guestSessionToken) {
      await loadGuestSession(true)
    } else {
      scheduleRequestPoll()
    }
    ElMessage.success(t('meet.guest.join.requestSubmitted'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('meet.guest.join.requestFailed'))
  } finally {
    submitting.value = false
  }
}

async function updateMedia(nextAudioEnabled: boolean, nextVideoEnabled: boolean): Promise<void> {
  if (!sessionToken.value || !guestSession.value?.selfParticipant) {
    return
  }
  updatingMedia.value = true
  try {
    guestSession.value = await updateMeetGuestSessionMedia(sessionToken.value, {
      audioEnabled: nextAudioEnabled,
      videoEnabled: nextVideoEnabled,
      screenSharing: guestSession.value.selfParticipant.screenSharing
    })
  } catch (error) {
    ElMessage.error((error as Error).message || t('meet.guest.join.mediaFailed'))
  } finally {
    updatingMedia.value = false
  }
}

async function toggleAudio(): Promise<void> {
  if (!guestSession.value?.selfParticipant) {
    return
  }
  await updateMedia(
    !guestSession.value.selfParticipant.audioEnabled,
    guestSession.value.selfParticipant.videoEnabled
  )
}

async function toggleVideo(): Promise<void> {
  if (!guestSession.value?.selfParticipant) {
    return
  }
  await updateMedia(
    guestSession.value.selfParticipant.audioEnabled,
    !guestSession.value.selfParticipant.videoEnabled
  )
}

async function sendHeartbeat(): Promise<void> {
  if (!sessionToken.value) {
    return
  }
  heartbeating.value = true
  try {
    guestSession.value = await heartbeatMeetGuestSession(sessionToken.value)
    ElMessage.success(t('meet.guest.join.heartbeatDone'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('meet.guest.join.heartbeatFailed'))
  } finally {
    heartbeating.value = false
  }
}

async function leaveSession(): Promise<void> {
  if (!sessionToken.value) {
    return
  }
  leaving.value = true
  try {
    guestSession.value = await leaveMeetGuestSession(sessionToken.value)
    clearSessionPollTimer()
    ElMessage.success(t('meet.guest.join.left'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('meet.guest.join.leaveFailed'))
  } finally {
    leaving.value = false
  }
}

onMounted(() => {
  void initializeGuestFlow()
})

onBeforeUnmount(() => {
  clearRequestPollTimer()
  clearSessionPollTimer()
})
</script>

<template>
  <div class="meet-guest-page">
    <header class="meet-guest-page__header">
      <NuxtLink to="/" class="meet-guest-page__brand">MMMail</NuxtLink>
      <LocaleSwitcher size="small" />
    </header>

    <main class="meet-guest-page__main" v-loading="loadingOverview">
      <section class="meet-guest-hero">
        <el-tag size="small" effect="plain" type="info">{{ t('meet.guest.join.badge') }}</el-tag>
        <h1>{{ t('meet.guest.join.title') }}</h1>
        <p>{{ t('meet.guest.join.subtitle') }}</p>
        <div v-if="overview" class="meet-guest-hero__facts">
          <span>{{ t('meet.guest.join.roomCode', { value: overview.roomCode }) }}</span>
          <span>{{ t('meet.guest.join.capacity', { current: overview.activeParticipants, max: overview.maxParticipants }) }}</span>
          <span>{{ t('meet.guest.join.access', { value: overviewAccessLabel }) }}</span>
        </div>
      </section>

      <section class="meet-guest-card">
        <template v-if="viewState === 'FORM'">
          <div class="meet-guest-card__head">
            <h2>{{ t('meet.guest.join.preflightTitle') }}</h2>
            <p>{{ t('meet.guest.join.preflightDescription') }}</p>
          </div>
          <el-alert
            v-if="overview && !overview.guestJoinEnabled"
            type="warning"
            :closable="false"
            :title="t('meet.guest.join.disabledTitle')"
            :description="t('meet.guest.join.disabledDescription')"
            show-icon
          />
          <el-form label-position="top">
            <el-form-item :label="t('meet.guest.join.name')">
              <el-input v-model="form.displayName" maxlength="64" />
            </el-form-item>
          </el-form>
          <div class="meet-guest-card__toggles">
            <el-button :type="form.audioEnabled ? 'success' : 'info'" plain @click="form.audioEnabled = !form.audioEnabled">
              {{ form.audioEnabled ? t('meet.guest.join.audioOn') : t('meet.guest.join.audioOff') }}
            </el-button>
            <el-button :type="form.videoEnabled ? 'success' : 'info'" plain @click="form.videoEnabled = !form.videoEnabled">
              {{ form.videoEnabled ? t('meet.guest.join.videoOn') : t('meet.guest.join.videoOff') }}
            </el-button>
          </div>
          <el-button
            class="meet-guest-card__submit"
            type="primary"
            :disabled="Boolean(overview && !overview.guestJoinEnabled)"
            :loading="submitting"
            @click="submitRequest"
          >
            {{ t('meet.guest.join.submit') }}
          </el-button>
        </template>

        <template v-else-if="viewState === 'WAITING'">
          <div class="meet-guest-card__head">
            <h2>{{ t('meet.guest.join.waitTitle') }}</h2>
            <p>{{ t('meet.guest.join.waitDescription') }}</p>
          </div>
          <el-tag :type="sessionTagType" effect="plain">
            {{ t('meet.guest.join.waitingState') }}
          </el-tag>
          <p class="meet-guest-card__summary">
            {{ t('meet.guest.join.waitSummary', { name: guestRequest?.displayName || form.displayName }) }}
          </p>
          <el-button :loading="pollingRequest" plain @click="loadGuestRequest(false)">
            {{ t('common.actions.refresh') }}
          </el-button>
        </template>

        <template v-else-if="viewState === 'REJECTED'">
          <div class="meet-guest-card__head">
            <h2>{{ t('meet.guest.join.rejectedTitle') }}</h2>
            <p>{{ t('meet.guest.join.rejectedDescription') }}</p>
          </div>
          <el-alert type="error" :closable="false" :title="t('meet.guest.join.rejectedState')" show-icon />
        </template>

        <template v-else>
          <div class="meet-guest-card__head">
            <h2>{{ t('meet.guest.join.sessionTitle') }}</h2>
            <p>{{ t('meet.guest.join.sessionDescription') }}</p>
          </div>
          <div class="meet-guest-session__status">
            <span>{{ t('meet.guest.join.sessionStatusLabel') }}</span>
            <el-tag :type="sessionTagType" effect="plain">{{ sessionStatusText }}</el-tag>
          </div>
          <el-alert
            v-if="viewState === 'ROOM_ENDED'"
            type="info"
            :closable="false"
            :title="t('meet.guest.join.roomEndedTitle')"
            :description="t('meet.guest.join.roomEndedDescription')"
            show-icon
          />
          <el-alert
            v-else-if="viewState === 'REMOVED'"
            type="warning"
            :closable="false"
            :title="t('meet.guest.join.removedTitle')"
            :description="t('meet.guest.join.removedDescription')"
            show-icon
          />
          <div class="meet-guest-session__panel" v-loading="loadingSession">
            <div class="meet-guest-session__controls">
              <el-button
                :disabled="!guestSession?.selfParticipant || !canManageSession"
                :loading="updatingMedia"
                :type="guestSession?.selfParticipant?.audioEnabled ? 'success' : 'info'"
                @click="toggleAudio"
              >
                {{ guestSession?.selfParticipant?.audioEnabled ? t('meet.guest.join.audioOn') : t('meet.guest.join.audioOff') }}
              </el-button>
              <el-button
                :disabled="!guestSession?.selfParticipant || !canManageSession"
                :loading="updatingMedia"
                :type="guestSession?.selfParticipant?.videoEnabled ? 'success' : 'info'"
                @click="toggleVideo"
              >
                {{ guestSession?.selfParticipant?.videoEnabled ? t('meet.guest.join.videoOn') : t('meet.guest.join.videoOff') }}
              </el-button>
              <el-button :disabled="!canManageSession" :loading="heartbeating" plain @click="sendHeartbeat">{{ t('meet.guest.join.heartbeat') }}</el-button>
              <el-button :disabled="viewState === 'ROOM_ENDED'" type="danger" plain :loading="leaving" @click="leaveSession">{{ t('meet.guest.join.leave') }}</el-button>
            </div>
            <div class="meet-guest-session__participants">
              <div class="meet-guest-session__participants-head">
                <h3>{{ t('meet.guest.join.participantsTitle') }}</h3>
                <span>{{ guestSession?.participants.length || 0 }}</span>
              </div>
              <el-table :data="guestSession?.participants || []" style="width: 100%">
                <el-table-column prop="displayName" :label="t('meet.guest.join.columns.name')" min-width="150" />
                <el-table-column prop="role" :label="t('meet.guest.join.columns.role')" min-width="120" />
                <el-table-column :label="t('meet.guest.join.columns.media')" min-width="190">
                  <template #default="scope">
                    <div class="meet-guest-session__media-tags">
                      <el-tag size="small" :type="scope.row.audioEnabled ? 'success' : 'info'">
                        {{ scope.row.audioEnabled ? t('meet.guest.join.audioOn') : t('meet.guest.join.audioOff') }}
                      </el-tag>
                      <el-tag size="small" :type="scope.row.videoEnabled ? 'success' : 'info'">
                        {{ scope.row.videoEnabled ? t('meet.guest.join.videoOn') : t('meet.guest.join.videoOff') }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column :label="t('meet.guest.join.columns.self')" min-width="100">
                  <template #default="scope">
                    <el-tag v-if="scope.row.self" type="success" effect="plain">{{ t('meet.guest.join.you') }}</el-tag>
                    <span v-else>—</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>
