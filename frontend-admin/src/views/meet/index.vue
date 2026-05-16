<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NSelect,
  NSpace,
  NStatistic,
  NSwitch,
  NTag
} from 'naive-ui';
import {
  activateMeetAccess,
  approveMeetGuestRequest,
  createMeetRoom,
  endMeetRoom,
  joinMeetRoom,
  joinMeetWaitlist,
  listMeetGuestRequests,
  listMeetParticipants,
  listMeetRoomHistory,
  listMeetRoomQuality,
  listMeetRooms,
  listMeetSignals,
  readCurrentMeetRoom,
  readMeetAccessOverview,
  readPublicMeetGuestRequest,
  readPublicMeetJoinOverview,
  rejectMeetGuestRequest,
  reportMeetParticipantQuality,
  requestMeetEnterpriseAccess,
  rotateMeetJoinCode,
  sendMeetSignalAnswer,
  sendMeetSignalIce,
  sendMeetSignalOffer,
  streamMeetSignals,
  submitPublicMeetGuestRequest,
  updateMeetParticipantMedia,
  updatePublicMeetGuestMedia,
  heartbeatPublicMeetGuestSession,
  leavePublicMeetGuestSession
} from '@/service/api';
import { $t } from '@/locales';
defineOptions({ name: 'Meet' });
const DEFAULT_MAX_PARTICIPANTS = 20;
const DEFAULT_SEATS = 10;
const DEFAULT_LIMIT = 20;
const route = useRoute();
const router = useRouter();
const access = ref<Api.Meet.AccessOverview | null>(null);
const currentRoom = ref<Api.Meet.Room | null>(null);
const rooms = ref<Api.Meet.Room[]>([]);
const history = ref<Api.Meet.Room[]>([]);
const participants = ref<Api.Meet.Participant[]>([]);
const guestRequests = ref<Api.Meet.GuestRequest[]>([]);
const qualitySnapshots = ref<Api.Meet.QualitySnapshot[]>([]);
const signals = ref<Api.Meet.SignalEvent[]>([]);
const publicJoinOverview = ref<Api.Meet.GuestJoinOverview | null>(null);
const publicGuestRequest = ref<Api.Meet.GuestRequest | null>(null);
const publicGuestSession = ref<Api.Meet.GuestSession | null>(null);
const selectedRoomId = ref('');
const selectedParticipantId = ref('');
const guestSessionToken = ref('');
const requestToken = ref('');
const roomModel = reactive<Api.Meet.RoomPayload>({
  accessLevel: 'PRIVATE',
  maxParticipants: DEFAULT_MAX_PARTICIPANTS,
  topic: ''
});
const accessModel = reactive<Api.Meet.EnterpriseAccessPayload>({
  companyName: '',
  requestedSeats: DEFAULT_SEATS,
  note: ''
});
const joinModel = reactive<Api.Meet.JoinPayload>({ displayName: '' });
const mediaModel = reactive<Api.Meet.MediaPayload>({ audioEnabled: true, videoEnabled: true, screenSharing: false });
const guestRequestModel = reactive<Api.Meet.GuestRequestPayload & { joinCode: string }>({
  joinCode: '',
  displayName: '',
  audioEnabled: true,
  videoEnabled: true
});
const signalModel = reactive<Api.Meet.SignalPayload>({ fromParticipantId: '', toParticipantId: '', payload: '' });
const qualityModel = reactive<Api.Meet.QualityPayload>({ jitterMs: 0, packetLossPercent: 0, roundTripMs: 0 });
const accessOptions = [
  { label: 'PRIVATE', value: 'PRIVATE' },
  { label: 'PUBLIC', value: 'PUBLIC' }
];
const activeRoomId = computed(() => selectedRoomId.value || currentRoom.value?.roomId || rooms.value[0]?.roomId || '');
const activeParticipantId = computed(() => selectedParticipantId.value || participants.value[0]?.participantId || '');
const sections = computed(() => [
  { name: 'meet_access' as const, label: $t('page.meet.access') },
  { name: 'meet_rooms' as const, label: $t('page.meet.rooms') },
  { name: 'meet_room_lobby' as const, label: $t('page.meet.lobby') },
  { name: 'meet_host' as const, label: $t('page.meet.host') }
]);
type MeetSectionName = (typeof sections.value)[number]['name'];
const roomColumns = computed(() => [
  { title: $t('page.meet.topic'), key: 'topic' },
  { title: $t('page.meet.accessLevel'), key: 'accessLevel' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.meet.joinCode'), key: 'joinCode' },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Meet.Room) => h(NSpace, { size: 8 }, { default: () => renderRoomActions(row) })
  }
]);
const participantColumns = computed(() => [
  { title: $t('page.meet.displayName'), key: 'displayName' },
  { title: $t('page.settings.mailAddressMode'), key: 'role' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.meet.audio'), key: 'audioEnabled' },
  { title: $t('page.meet.video'), key: 'videoEnabled' }
]);
const guestColumns = computed(() => [
  { title: $t('page.meet.displayName'), key: 'displayName' },
  { title: $t('page.notifications.status'), key: 'status' },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Meet.GuestRequest) => h(NSpace, { size: 8 }, { default: () => renderGuestActions(row) })
  }
]);
const signalColumns = computed(() => [
  { title: 'Seq', key: 'eventSeq' },
  { title: $t('page.wallet.type'), key: 'signalType' },
  { title: $t('page.meet.payload'), key: 'payload' }
]);
const qualityColumns = computed(() => [
  { title: $t('page.meet.displayName'), key: 'participantId' },
  { title: $t('page.meet.quality'), key: 'qualityScore' },
  { title: $t('page.meet.jitter'), key: 'jitterMs' },
  { title: $t('page.meet.packetLoss'), key: 'packetLossPercent' }
]);
function renderRoomActions(row: Api.Meet.Room) {
  return [
    h(NButton, { size: 'small', onClick: () => selectRoom(row.roomId) }, { default: () => $t('common.select') }),
    h(
      NButton,
      { size: 'small', onClick: () => rotateMeetJoinCode(row.roomId).then(loadMeet) },
      { default: () => $t('page.meet.rotateJoinCode') }
    ),
    h(
      NButton,
      { size: 'small', type: 'error', onClick: () => endMeetRoom(row.roomId).then(loadMeet) },
      { default: () => $t('page.meet.endRoom') }
    )
  ];
}
function renderGuestActions(row: Api.Meet.GuestRequest) {
  return [
    h(
      NButton,
      { size: 'small', type: 'primary', onClick: () => approveGuest(row.requestId) },
      { default: () => $t('page.meet.approve') }
    ),
    h(
      NButton,
      { size: 'small', type: 'error', onClick: () => rejectGuest(row.requestId) },
      { default: () => $t('page.meet.reject') }
    )
  ];
}
function routeIsActive(target: MeetSectionName) {
  const currentRoute = String(route.name || 'meet_access');
  return currentRoute === target || (currentRoute === 'meet' && target === 'meet_access');
}
function sectionButtonType(target: MeetSectionName) {
  return routeIsActive(target) ? 'primary' : 'default';
}
function openSection(target: MeetSectionName) {
  const params = target === 'meet_room_lobby' || target === 'meet_host' ? { roomId: activeRoomId.value } : undefined;
  router.push({ name: target, params });
}
function routeParam(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] || '' : value || '';
}
async function loadRoomRuntime(roomId = activeRoomId.value) {
  if (!roomId) return;
  selectedRoomId.value = roomId;
  const [participantResult, guestResult, qualityResult, signalResult] = await Promise.all([
    listMeetParticipants(roomId),
    listMeetGuestRequests(roomId),
    listMeetRoomQuality(roomId, { limit: DEFAULT_LIMIT }),
    listMeetSignals(roomId, { limit: DEFAULT_LIMIT })
  ]);
  if (!participantResult.error) participants.value = participantResult.data;
  if (!guestResult.error) guestRequests.value = guestResult.data;
  if (!qualityResult.error) qualitySnapshots.value = qualityResult.data;
  if (!signalResult.error) signals.value = signalResult.data;
}
async function loadMeet() {
  const [accessResult, roomsResult, currentResult, historyResult] = await Promise.all([
    readMeetAccessOverview(),
    listMeetRooms({ limit: DEFAULT_LIMIT }),
    readCurrentMeetRoom(),
    listMeetRoomHistory({ limit: DEFAULT_LIMIT })
  ]);
  if (!accessResult.error) access.value = accessResult.data;
  if (!roomsResult.error) rooms.value = roomsResult.data;
  if (!currentResult.error) currentRoom.value = currentResult.data;
  if (!historyResult.error) history.value = historyResult.data;
  await loadRoomRuntime();
}
async function selectRoom(roomId: string) {
  selectedRoomId.value = roomId;
  await loadRoomRuntime(roomId);
}
async function submitRoom() {
  const { error } = await createMeetRoom({ ...roomModel });
  if (!error) {
    roomModel.topic = '';
    await loadMeet();
  }
}
async function submitWaitlist() {
  const { data, error } = await joinMeetWaitlist({ note: accessModel.note });
  if (!error) access.value = data;
}
async function submitSalesRequest() {
  const { data, error } = await requestMeetEnterpriseAccess({ ...accessModel });
  if (!error) access.value = data;
}
async function submitActivation() {
  const { data, error } = await activateMeetAccess();
  if (!error) access.value = data;
}
async function submitJoinRoom() {
  const roomId = activeRoomId.value;
  if (!roomId) return;
  const { data, error } = await joinMeetRoom(roomId, { ...joinModel });
  if (!error) {
    selectedParticipantId.value = data.participantId;
    await loadRoomRuntime(roomId);
  }
}
async function submitMedia() {
  const roomId = activeRoomId.value;
  const participantId = activeParticipantId.value;
  if (roomId && participantId) {
    await updateMeetParticipantMedia(roomId, participantId, { ...mediaModel });
    await loadRoomRuntime(roomId);
  }
}
async function submitQuality() {
  const roomId = activeRoomId.value;
  const participantId = activeParticipantId.value;
  if (roomId && participantId) {
    await reportMeetParticipantQuality(roomId, participantId, { ...qualityModel });
    await loadRoomRuntime(roomId);
  }
}
async function approveGuest(requestId: string) {
  if (!activeRoomId.value) return;
  await approveMeetGuestRequest(activeRoomId.value, requestId);
  await loadRoomRuntime(activeRoomId.value);
}
async function rejectGuest(requestId: string) {
  if (!activeRoomId.value) return;
  await rejectMeetGuestRequest(activeRoomId.value, requestId);
  await loadRoomRuntime(activeRoomId.value);
}
async function submitPublicJoinOverview() {
  const { data, error } = await readPublicMeetJoinOverview(guestRequestModel.joinCode);
  if (!error) publicJoinOverview.value = data;
}
async function submitPublicGuestRequest() {
  const { data, error } = await submitPublicMeetGuestRequest(guestRequestModel.joinCode, { ...guestRequestModel });
  if (!error) {
    publicGuestRequest.value = data;
    requestToken.value = data.requestToken;
  }
}
async function refreshPublicGuestRequest() {
  if (!requestToken.value) return;
  const { data, error } = await readPublicMeetGuestRequest(requestToken.value);
  if (!error) {
    publicGuestRequest.value = data;
    guestSessionToken.value = data.guestSessionToken || guestSessionToken.value;
  }
}
async function heartbeatGuestSession() {
  if (!guestSessionToken.value) return;
  const { data, error } = await heartbeatPublicMeetGuestSession(guestSessionToken.value);
  if (!error) publicGuestSession.value = data;
}
async function updateGuestMedia() {
  if (!guestSessionToken.value) return;
  const { data, error } = await updatePublicMeetGuestMedia(guestSessionToken.value, { ...mediaModel });
  if (!error) publicGuestSession.value = data;
}
async function leaveGuestSession() {
  if (!guestSessionToken.value) return;
  const { data, error } = await leavePublicMeetGuestSession(guestSessionToken.value);
  if (!error) publicGuestSession.value = data;
}
async function sendSignal(kind: 'offer' | 'answer' | 'ice') {
  const roomId = activeRoomId.value;
  if (!roomId) return;
  if (kind === 'offer') await sendMeetSignalOffer(roomId, { ...signalModel });
  if (kind === 'answer') await sendMeetSignalAnswer(roomId, { ...signalModel });
  if (kind === 'ice') await sendMeetSignalIce(roomId, { ...signalModel });
  await loadRoomRuntime(roomId);
}
async function streamSignalsNow() {
  const roomId = activeRoomId.value;
  if (!roomId) return;
  const { data, error } = await streamMeetSignals(roomId, { limit: DEFAULT_LIMIT, timeoutSeconds: 1 });
  if (!error) signals.value = data;
}
onMounted(async () => {
  selectedRoomId.value = routeParam(route.params.roomId);
  guestRequestModel.joinCode = routeParam(route.params.joinCode);
  await loadMeet();
  if (guestRequestModel.joinCode) await submitPublicJoinOverview();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard class="card-wrapper" :title="$t('route.meet')">
      <NSpace justify="space-between" align="center">
        <NSpace>
          <NTag>{{ access?.accessState || $t('common.noData') }}</NTag>
          <NTag type="success">{{ currentRoom?.topic || $t('common.noData') }}</NTag>
          <NTag>{{ activeRoomId || '-' }}</NTag>
        </NSpace>
        <NSpace>
          <NButton
            v-for="section in sections"
            :key="section.name"
            :type="sectionButtonType(section.name)"
            @click="openSection(section.name)"
          >
            {{ section.label }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>
    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:15">
        <NCard class="card-wrapper" :title="$t('page.meet.rooms')">
          <NSpace class="mb-12px">
            <NStatistic :label="$t('page.meet.plan')" :value="access?.planName || $t('common.noData')" />
            <NStatistic :label="$t('page.meet.current')" :value="currentRoom?.topic || $t('common.noData')" />
          </NSpace>
          <NDataTable :columns="roomColumns" :data="rooms" />
        </NCard>
        <NCard class="card-wrapper mt-16px" :title="$t('page.meet.history')">
          <NDataTable :columns="roomColumns" :data="history" />
        </NCard>
        <NCard class="card-wrapper mt-16px" :title="$t('page.meet.participants')">
          <NDataTable :columns="participantColumns" :data="participants" />
        </NCard>
        <NCard class="card-wrapper mt-16px" :title="$t('page.meet.guestRequests')">
          <NDataTable :columns="guestColumns" :data="guestRequests" />
        </NCard>
        <NCard class="card-wrapper mt-16px" :title="$t('page.meet.signals')">
          <NDataTable :columns="signalColumns" :data="signals" />
        </NCard>
        <NCard class="card-wrapper mt-16px" :title="$t('page.meet.quality')">
          <NDataTable :columns="qualityColumns" :data="qualitySnapshots" />
        </NCard>
      </NGi>
      <NGi span="24 l:9">
        <NSpace vertical :size="16">
          <NCard class="card-wrapper" :title="$t('page.meet.access')">
            <NForm :model="accessModel" label-placement="top">
              <NFormItem path="companyName" :label="$t('page.meet.company')">
                <NInput v-model:value="accessModel.companyName" />
              </NFormItem>
              <NFormItem path="requestedSeats" :label="$t('page.meet.seats')">
                <NInputNumber v-model:value="accessModel.requestedSeats" class="w-full" />
              </NFormItem>
              <NFormItem path="note" :label="$t('page.meet.note')">
                <NInput v-model:value="accessModel.note" type="textarea" />
              </NFormItem>
              <NSpace>
                <NButton @click="submitWaitlist">{{ $t('page.meet.waitlist') }}</NButton>
                <NButton @click="submitSalesRequest">{{ $t('page.meet.contactSales') }}</NButton>
                <NButton type="primary" @click="submitActivation">{{ $t('page.meet.activate') }}</NButton>
              </NSpace>
            </NForm>
          </NCard>
          <NCard class="card-wrapper" :title="$t('page.meet.create')">
            <NForm :model="roomModel" label-placement="top">
              <NFormItem path="topic" :label="$t('page.meet.topic')">
                <NInput v-model:value="roomModel.topic" />
              </NFormItem>
              <NFormItem path="accessLevel" :label="$t('page.meet.accessLevel')">
                <NSelect v-model:value="roomModel.accessLevel" :options="accessOptions" />
              </NFormItem>
              <NFormItem path="maxParticipants" :label="$t('page.meet.maxParticipants')">
                <NInputNumber v-model:value="roomModel.maxParticipants" class="w-full" :min="2" :max="200" />
              </NFormItem>
              <NButton type="primary" @click="submitRoom">{{ $t('page.meet.create') }}</NButton>
            </NForm>
          </NCard>
          <NCard class="card-wrapper" :title="$t('page.meet.lobby')">
            <NForm :model="mediaModel" label-placement="top">
              <NFormItem path="displayName" :label="$t('page.meet.displayName')">
                <NInput v-model:value="joinModel.displayName" />
              </NFormItem>
              <NFormItem path="audioEnabled" :label="$t('page.meet.audio')">
                <NSwitch v-model:value="mediaModel.audioEnabled" />
              </NFormItem>
              <NFormItem path="videoEnabled" :label="$t('page.meet.video')">
                <NSwitch v-model:value="mediaModel.videoEnabled" />
              </NFormItem>
              <NFormItem path="screenSharing" :label="$t('page.meet.screen')">
                <NSwitch v-model:value="mediaModel.screenSharing" />
              </NFormItem>
              <NSpace>
                <NButton type="primary" @click="submitJoinRoom">{{ $t('page.meet.join') }}</NButton>
                <NButton @click="submitMedia">{{ $t('common.update') }}</NButton>
              </NSpace>
            </NForm>
          </NCard>
          <NCard class="card-wrapper" :title="$t('page.meet.join')">
            <NForm :model="guestRequestModel" label-placement="top">
              <NFormItem path="joinCode" :label="$t('page.meet.joinCode')">
                <NInput v-model:value="guestRequestModel.joinCode" />
              </NFormItem>
              <NFormItem path="displayName" :label="$t('page.meet.displayName')">
                <NInput v-model:value="guestRequestModel.displayName" />
              </NFormItem>
              <NSpace>
                <NButton @click="submitPublicJoinOverview">
                  {{ publicJoinOverview?.topic || $t('page.meet.join') }}
                </NButton>
                <NButton type="primary" @click="submitPublicGuestRequest">{{ $t('page.meet.guestRequests') }}</NButton>
                <NButton @click="refreshPublicGuestRequest">
                  {{ publicGuestRequest?.status || $t('common.refresh') }}
                </NButton>
              </NSpace>
              <NSpace class="mt-12px">
                <NButton @click="heartbeatGuestSession">{{ $t('page.meet.heartbeat') }}</NButton>
                <NButton @click="updateGuestMedia">{{ $t('common.update') }}</NButton>
                <NButton type="error" @click="leaveGuestSession">
                  {{ publicGuestSession?.sessionStatus || $t('common.close') }}
                </NButton>
              </NSpace>
            </NForm>
          </NCard>
          <NCard class="card-wrapper" :title="$t('page.meet.signals')">
            <NForm :model="signalModel" label-placement="top">
              <NFormItem path="fromParticipantId" label="From">
                <NInput v-model:value="signalModel.fromParticipantId" />
              </NFormItem>
              <NFormItem path="toParticipantId" label="To">
                <NInput v-model:value="signalModel.toParticipantId" />
              </NFormItem>
              <NFormItem path="payload" :label="$t('page.meet.payload')">
                <NInput v-model:value="signalModel.payload" type="textarea" />
              </NFormItem>
              <NSpace>
                <NButton @click="sendSignal('offer')">Offer</NButton>
                <NButton @click="sendSignal('answer')">Answer</NButton>
                <NButton @click="sendSignal('ice')">ICE</NButton>
                <NButton type="primary" @click="streamSignalsNow">{{ $t('page.meet.signals') }}</NButton>
              </NSpace>
            </NForm>
          </NCard>
          <NCard class="card-wrapper" :title="$t('page.meet.quality')">
            <NForm :model="qualityModel" label-placement="top">
              <NFormItem path="jitterMs" :label="$t('page.meet.jitter')">
                <NInputNumber v-model:value="qualityModel.jitterMs" class="w-full" />
              </NFormItem>
              <NFormItem path="packetLossPercent" :label="$t('page.meet.packetLoss')">
                <NInputNumber v-model:value="qualityModel.packetLossPercent" class="w-full" />
              </NFormItem>
              <NFormItem path="roundTripMs" :label="$t('page.meet.roundTrip')">
                <NInputNumber v-model:value="qualityModel.roundTripMs" class="w-full" />
              </NFormItem>
              <NButton type="primary" @click="submitQuality">{{ $t('page.meet.quality') }}</NButton>
            </NForm>
          </NCard>
        </NSpace>
      </NGi>
    </NGrid>
  </NSpace>
</template>
