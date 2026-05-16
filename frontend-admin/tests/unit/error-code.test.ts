import { describe, expect, it } from 'vitest';
import { zhCNErrorMessages, enUSErrorMessages } from '@/locales/langs/v212-error-messages';

const SEGMENT_RANGES: Array<{ name: string; min: number; max: number }> = [
  { name: 'auth (10000-19999)', min: 10000, max: 19999 },
  { name: 'authorization (20000-29999)', min: 20000, max: 29999 },
  { name: 'platform/entitlement (30000-39999)', min: 30000, max: 39999 },
  { name: 'mail/community (40000-49999)', min: 40000, max: 49999 },
  { name: 'wallet/billing (50000-59999)', min: 50000, max: 59999 },
  { name: 'meet/vpn (60000-69999)', min: 60000, max: 69999 },
  { name: 'docs/drive (70000-79999)', min: 70000, max: 79999 },
  { name: 'integrations (80000-89999)', min: 80000, max: 89999 }
];

describe('error-code i18n parity', () => {
  it('zh-CN and en-US declare the exact same set of error codes', () => {
    const zhKeys = Object.keys(zhCNErrorMessages).sort();
    const enKeys = Object.keys(enUSErrorMessages).sort();
    expect(zhKeys).toEqual(enKeys);
  });

  it('every error code is a 5-digit numeric string and lands in a known segment', () => {
    for (const code of Object.keys(zhCNErrorMessages)) {
      expect(code).toMatch(/^\d{5}$/);
      const numeric = Number(code);
      const segment = SEGMENT_RANGES.find(r => numeric >= r.min && numeric <= r.max);
      expect(segment, `code ${code} has no segment`).toBeTruthy();
    }
  });

  it('each entry carries non-empty title and message text in both languages', () => {
    const validate = (map: Record<string, { title: string; message: string }>, label: string) => {
      for (const [code, entry] of Object.entries(map)) {
        expect(entry.title.length, `${label}:${code}.title`).toBeGreaterThan(0);
        expect(entry.message.length, `${label}:${code}.message`).toBeGreaterThan(0);
      }
    };
    validate(zhCNErrorMessages as never, 'zh-CN');
    validate(enUSErrorMessages as never, 'en-US');
  });
});
