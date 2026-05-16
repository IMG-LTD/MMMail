import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 meet service exposes access, room lifecycle, participant, guest, public, and signal APIs', async () => {
  const service = await read('src/service/api/meet.ts');

  for (const apiName of [
    'joinMeetWaitlist',
    'requestMeetEnterpriseAccess',
    'activateMeetAccess',
    'listMeetRoomHistory',
    'rotateMeetJoinCode',
    'endMeetRoom',
    'listMeetParticipants',
    'joinMeetRoom',
    'leaveMeetParticipant',
    'heartbeatMeetParticipant',
    'updateMeetParticipantMedia',
    'reportMeetParticipantQuality',
    'listMeetRoomQuality',
    'listMeetGuestRequests',
    'approveMeetGuestRequest',
    'rejectMeetGuestRequest',
    'readPublicMeetJoinOverview',
    'submitPublicMeetGuestRequest',
    'readPublicMeetGuestRequest',
    'readPublicMeetGuestSession',
    'heartbeatPublicMeetGuestSession',
    'updatePublicMeetGuestMedia',
    'leavePublicMeetGuestSession',
    'sendMeetSignalOffer',
    'sendMeetSignalAnswer',
    'sendMeetSignalIce',
    'listMeetSignals',
    'streamMeetSignals'
  ]) {
    assert.match(service, new RegExp(apiName));
  }

  assert.match(service, /\/api\/v1\/meet\/rooms\/\$\{roomId\}\/join-code\/rotate/);
  assert.match(service, /\/api\/v1\/meet\/rooms\/\$\{roomId\}\/participants\/\$\{participantId\}\/media/);
  assert.match(service, /\/api\/v1\/public\/meet\/join\/\$\{joinCode\}/);
  assert.match(service, /\/api\/v1\/meet\/rooms\/\$\{roomId\}\/signals\/stream/);
});

test('v2.1.2 meet routes expose access, room, lobby, join, and host entry points', async () => {
  const routes = await read('src/router/routes/custom-routes.ts');

  for (const routeName of [
    'meet_access',
    'meet_rooms',
    'meet_room_detail',
    'meet_room_lobby',
    'meet_join',
    'meet_host'
  ]) {
    assert.match(routes, new RegExp(`name: '${routeName}'`));
  }

  assert.match(routes, /path: '\/meet\/access'/);
  assert.match(routes, /path: '\/meet\/rooms'/);
  assert.match(routes, /path: '\/meet\/rooms\/:roomId'/);
  assert.match(routes, /path: '\/meet\/rooms\/:roomId\/lobby'/);
  assert.match(routes, /path: '\/meet\/join\/:joinCode'/);
  assert.match(routes, /path: '\/meet\/host\/:roomId'/);
  assert.match(routes, /requires: \['MEET'\]/);
  assert.match(routes, /featureFlag: 'feat\.meet\.enabled'/);
});

test('v2.1.2 meet page binds lifecycle, media, guest approval, public join, and signal workflows', async () => {
  const page = await read('src/views/meet/index.vue');

  for (const token of [
    'joinMeetWaitlist',
    'activateMeetAccess',
    'rotateMeetJoinCode',
    'endMeetRoom',
    'listMeetParticipants',
    'joinMeetRoom',
    'updateMeetParticipantMedia',
    'listMeetGuestRequests',
    'approveMeetGuestRequest',
    'readPublicMeetJoinOverview',
    'submitPublicMeetGuestRequest',
    'listMeetSignals',
    'streamMeetSignals',
    'mediaModel',
    'guestRequestModel',
    'signalModel',
    'qualityModel'
  ]) {
    assert.match(page, new RegExp(token));
  }
});
