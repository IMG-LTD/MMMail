import enUS from '../src/locales/langs/en-us';
import zhCN from '../src/locales/langs/zh-cn';
import zhTW from '../src/locales/langs/zh-tw';

const locales = {
  'en-US': flatten(enUS),
  'zh-CN': flatten(zhCN),
  'zh-TW': flatten(zhTW)
};

const referenceKeys = locales['zh-CN'];
const failures = Object.entries(locales).flatMap(([locale, keys]) => compareKeys(locale, referenceKeys, keys));

if (failures.length) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log(`i18n key checks passed: ${referenceKeys.size} keys`);
}

function compareKeys(locale: string, expected: Set<string>, actual: Set<string>) {
  const missing = [...expected].filter(key => !actual.has(key)).map(key => `${locale} missing ${key}`);
  const extra = [...actual].filter(key => !expected.has(key)).map(key => `${locale} extra ${key}`);
  return [...missing, ...extra];
}

function flatten(value: unknown, prefix = ''): Set<string> {
  if (!isRecord(value)) {
    return new Set([prefix]);
  }

  return Object.entries(value).reduce((keys, [key, child]) => {
    const childPrefix = prefix ? `${prefix}.${key}` : key;
    for (const item of flatten(child, childPrefix)) {
      keys.add(item);
    }
    return keys;
  }, new Set<string>());
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
