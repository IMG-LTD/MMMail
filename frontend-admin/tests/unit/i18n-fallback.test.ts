import { describe, expect, it } from 'vitest';
import { createI18n } from 'vue-i18n';
import zhCN from '@/locales/langs/zh-cn';
import enUS from '@/locales/langs/en-us';

describe('i18n fallback behaviour', () => {
  it('falls back to en when a key is missing in zh-CN runtime overrides', () => {
    const stripped = JSON.parse(JSON.stringify(zhCN)) as typeof zhCN;
    delete (stripped.system as Partial<typeof stripped.system>).updateTitle;

    const i18n = createI18n({
      legacy: false,
      locale: 'zh-CN',
      fallbackLocale: 'en',
      missingWarn: false,
      fallbackWarn: false,
      messages: {
        'zh-CN': stripped,
        en: enUS
      }
    });

    expect(i18n.global.t('system.updateTitle')).toBe(enUS.system.updateTitle);
  });

  it('serves the zh-CN translation when both locales define the key', () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'zh-CN',
      fallbackLocale: 'en',
      missingWarn: false,
      fallbackWarn: false,
      messages: { 'zh-CN': zhCN, en: enUS }
    });
    expect(i18n.global.t('system.updateTitle')).toBe(zhCN.system.updateTitle);
  });
});
