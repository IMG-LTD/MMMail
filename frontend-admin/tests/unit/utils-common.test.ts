import { describe, expect, it } from 'vitest';
import { toggleHtmlClass, transformRecordToOption, translateOptions } from '@/utils/common';

describe('utils/common helpers', () => {
  it('transformRecordToOption maps every key/value pair to value/label option', () => {
    const record = { ready: 'Ready', running: 'Running', failed: 'Failed' };
    expect(transformRecordToOption(record)).toEqual([
      { value: 'ready', label: 'Ready' },
      { value: 'running', label: 'Running' },
      { value: 'failed', label: 'Failed' }
    ]);
  });

  it('translateOptions feeds each label to the configured i18n callable', () => {
    const options = [
      { value: 'a', label: 'common.add' as App.I18n.I18nKey },
      { value: 'b', label: 'common.cancel' as App.I18n.I18nKey }
    ];
    const translated = translateOptions(options);
    expect(translated.every(item => typeof item.label === 'string')).toBe(true);
    expect(translated[0].value).toBe('a');
    expect(translated[1].value).toBe('b');
  });

  it('returns an empty array when input record is empty', () => {
    expect(transformRecordToOption({})).toEqual([]);
  });

  it('toggleHtmlClass adds and removes the html class on document element', () => {
    const { add, remove } = toggleHtmlClass('mm-test-class');
    add();
    expect(document.documentElement.classList.contains('mm-test-class')).toBe(true);
    remove();
    expect(document.documentElement.classList.contains('mm-test-class')).toBe(false);
  });
});
