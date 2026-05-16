import { Awareness } from 'y-protocols/awareness';
import { ySyncPlugin } from 'y-prosemirror';
import * as Y from 'yjs';
import { getCollabSnapshot, writeCollabSnapshot } from '@/service/api';
import { getToken } from '@/store/modules/auth/shared';

const DOCS_RESOURCE_TYPE = 'docs';
const DOCS_XML_FRAGMENT = 'content';
const SNAPSHOT_DEBOUNCE_MS = 2_000;
const BASE64_CHUNK_SIZE = 32_768;
const HTTPS_PROTOCOL = 'https:';
const WSS_PROTOCOL = 'wss:';
const WS_PROTOCOL = 'ws:';
const REMOTE_UPDATE_ORIGIN = Symbol('docs-collab-remote-update');

interface DocsCollabSessionOptions {
  resourceId: string;
  resourceType?: string;
  doc?: Y.Doc;
  onError?: (error: Error) => void;
}

export interface DocsCollabSession {
  doc: Y.Doc;
  awareness: Awareness;
  bootstrap: () => Promise<Api.Docs.CollabSnapshot>;
  flushSnapshot: () => Promise<void>;
  destroy: () => void;
}

export function createDocsCollabSession(options: DocsCollabSessionOptions): DocsCollabSession {
  const resourceType = options.resourceType ?? DOCS_RESOURCE_TYPE;
  const doc = options.doc ?? new Y.Doc();
  const awareness = new Awareness(doc);
  let socket: WebSocket | null = null;
  let snapshotVersion = 0;
  let snapshotTimer: ReturnType<typeof setTimeout> | null = null;
  const pendingUpdates: Uint8Array[] = [];

  const handleUpdate = (update: Uint8Array, origin: unknown) => {
    if (origin === REMOTE_UPDATE_ORIGIN) {
      return;
    }
    sendUpdate(socket, update, pendingUpdates, options.onError);
    scheduleSnapshot();
  };

  async function bootstrap() {
    const snapshot = await loadSnapshot(resourceType, options.resourceId, doc);
    snapshotVersion = snapshot.version;
    const collabSocket = openSocket(resourceType, options.resourceId, options.onError);
    socket = collabSocket;
    collabSocket.binaryType = 'arraybuffer';
    collabSocket.addEventListener('open', () => flushPendingUpdates(collabSocket, pendingUpdates, options.onError));
    collabSocket.addEventListener('message', message => void applyRemoteMessage(doc, message).catch(options.onError));
    collabSocket.addEventListener('error', () => options.onError?.(new Error('docs collaboration websocket error')));
    doc.on('update', handleUpdate);
    return snapshot;
  }

  async function flushSnapshot() {
    clearSnapshotTimer();
    const snapshotBase64 = bytesToBase64(Y.encodeStateAsUpdate(doc));
    const response = await writeCollabSnapshot(resourceType, options.resourceId, {
      version: snapshotVersion + 1,
      snapshotBase64
    });
    if (response.error) {
      throw new Error('docs collaboration snapshot write failed');
    }
    snapshotVersion = response.data.version;
  }

  function scheduleSnapshot() {
    clearSnapshotTimer();
    snapshotTimer = setTimeout(() => void flushSnapshot().catch(options.onError), SNAPSHOT_DEBOUNCE_MS);
  }

  function clearSnapshotTimer() {
    if (snapshotTimer) {
      clearTimeout(snapshotTimer);
      snapshotTimer = null;
    }
  }

  function destroy() {
    clearSnapshotTimer();
    doc.off('update', handleUpdate);
    awareness.destroy();
    socket?.close();
    socket = null;
  }

  return { doc, awareness, bootstrap, flushSnapshot, destroy };
}

export function createDocsYSyncPlugin(doc: Y.Doc) {
  return ySyncPlugin(doc.getXmlFragment(DOCS_XML_FRAGMENT));
}

async function loadSnapshot(resourceType: string, resourceId: string, doc: Y.Doc) {
  const response = await getCollabSnapshot(resourceType, resourceId);
  if (response.error) {
    throw new Error('docs collaboration snapshot load failed');
  }
  if (response.data.snapshotBase64) {
    Y.applyUpdate(doc, base64ToBytes(response.data.snapshotBase64), REMOTE_UPDATE_ORIGIN);
  }
  return response.data;
}

function openSocket(resourceType: string, resourceId: string, onError?: (error: Error) => void) {
  const token = getToken();
  if (!token) {
    const error = new Error('docs collaboration token is required');
    onError?.(error);
    throw error;
  }
  return new WebSocket(buildDocsCollabWebSocketUrl(token, resourceType, resourceId));
}

function sendUpdate(
  socket: WebSocket | null,
  update: Uint8Array,
  pendingUpdates: Uint8Array[],
  onError?: (error: Error) => void
) {
  if (!socket) {
    onError?.(new Error('docs collaboration websocket is not open'));
    return;
  }
  if (socket.readyState === WebSocket.CONNECTING) {
    pendingUpdates.push(update);
    return;
  }
  if (socket.readyState !== WebSocket.OPEN) {
    onError?.(new Error('docs collaboration websocket is not open'));
    return;
  }
  socket.send(toArrayBuffer(update));
}

function flushPendingUpdates(socket: WebSocket, pendingUpdates: Uint8Array[], onError?: (error: Error) => void) {
  while (pendingUpdates.length > 0) {
    const update = pendingUpdates.shift();
    if (!update) {
      continue;
    }
    sendUpdate(socket, update, pendingUpdates, onError);
  }
}

async function applyRemoteMessage(doc: Y.Doc, message: MessageEvent) {
  const bytes = await readMessageBytes(message.data);
  if (!bytes) {
    return;
  }
  Y.applyUpdate(doc, bytes, REMOTE_UPDATE_ORIGIN);
}

async function readMessageBytes(data: unknown) {
  if (data instanceof ArrayBuffer) {
    return new Uint8Array(data);
  }
  if (data instanceof Uint8Array) {
    return data;
  }
  if (data instanceof Blob) {
    return new Uint8Array(await data.arrayBuffer());
  }
  return null;
}

function bytesToBase64(bytes: Uint8Array) {
  let binary = '';
  for (let index = 0; index < bytes.length; index += BASE64_CHUNK_SIZE) {
    binary += String.fromCharCode(...bytes.subarray(index, index + BASE64_CHUNK_SIZE));
  }
  return window.btoa(binary);
}

function base64ToBytes(value: string) {
  const binary = window.atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
}

function toArrayBuffer(bytes: Uint8Array) {
  const buffer = new ArrayBuffer(bytes.byteLength);
  new Uint8Array(buffer).set(bytes);
  return buffer;
}

export function buildDocsCollabWebSocketUrl(token: string, resourceType: string, resourceId: string) {
  const baseUrl = new URL(import.meta.env.VITE_SERVICE_BASE_URL || window.location.origin, window.location.origin);
  baseUrl.protocol = baseUrl.protocol === HTTPS_PROTOCOL ? WSS_PROTOCOL : WS_PROTOCOL;
  baseUrl.pathname = `/ws/collab/${resourceType}/${resourceId}`;
  baseUrl.search = '';
  baseUrl.searchParams.set('token', token);
  return baseUrl.toString();
}
