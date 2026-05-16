const storagePrefix = import.meta.env.VITE_STORAGE_PREFIX || '';

type BrowserStorageKind = 'local' | 'session';

export const localStg = createBrowserStorage<StorageType.Local>('local', storagePrefix);

export const sessionStg = createBrowserStorage<StorageType.Session>('session', storagePrefix);

function createBrowserStorage<T extends object>(type: BrowserStorageKind, prefix: string) {
  const stg = type === 'session' ? window.sessionStorage : window.localStorage;

  return {
    set<K extends keyof T>(key: K, value: T[K]) {
      stg.setItem(toStorageKey(prefix, key), JSON.stringify(value));
    },
    get<K extends keyof T>(key: K): T[K] | null {
      const storageKey = toStorageKey(prefix, key);
      const json = stg.getItem(storageKey);

      if (!json) {
        stg.removeItem(storageKey);
        return null;
      }

      const data = parseStorageJson<T[K]>(json);
      if (data !== null) {
        return data;
      }

      stg.removeItem(storageKey);
      return null;
    },
    remove(key: keyof T) {
      stg.removeItem(toStorageKey(prefix, key));
    },
    clear() {
      stg.clear();
    }
  };
}

function toStorageKey(prefix: string, key: PropertyKey) {
  return `${prefix}${String(key)}`;
}

function parseStorageJson<T>(json: string) {
  try {
    return JSON.parse(json) as T;
  } catch {
    return null;
  }
}
