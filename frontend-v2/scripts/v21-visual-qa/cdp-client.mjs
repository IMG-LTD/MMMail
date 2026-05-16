export class CdpClient {
  constructor(ws) {
    this.nextId = 1;
    this.pending = new Map();
    this.waiters = [];
    this.ws = ws;
    ws.addEventListener("message", (event) => this.handleMessage(event.data));
  }

  close() {
    this.ws.close();
  }

  handleMessage(data) {
    const text = typeof data === "string" ? data : data.toString();
    const message = JSON.parse(text);
    if (message.id && this.pending.has(message.id)) {
      this.resolvePending(message);
      return;
    }
    this.resolveWaiters(message);
  }

  resolvePending(message) {
    const pending = this.pending.get(message.id);
    this.pending.delete(message.id);
    if (message.error) {
      pending.reject(
        new Error(`${message.error.message}: ${JSON.stringify(message.error.data || "")}`),
      );
      return;
    }
    pending.resolve(message.result || {});
  }

  resolveWaiters(message) {
    this.waiters = this.waiters.filter((waiter) => {
      if (waiter.method !== message.method || waiter.sessionId !== message.sessionId) {
        return true;
      }
      clearTimeout(waiter.timer);
      waiter.resolve(message.params || {});
      return false;
    });
  }

  send(method, params = {}, sessionId) {
    const id = this.nextId++;
    const payload = sessionId ? { id, method, params, sessionId } : { id, method, params };
    this.ws.send(JSON.stringify(payload));
    return new Promise((resolve, reject) => {
      this.pending.set(id, { reject, resolve });
    });
  }

  waitForEvent(method, sessionId, timeoutMs) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(
        () => reject(new Error(`Timed out waiting for ${method}`)),
        timeoutMs,
      );
      this.waiters.push({ method, reject, resolve, sessionId, timer });
    });
  }
}
