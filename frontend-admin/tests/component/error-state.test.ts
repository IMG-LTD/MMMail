import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

import ErrorState from '@/components/feedback/ErrorState.vue';

const global = {
  stubs: {
    Empty: {
      props: ['description'],
      template: '<div class="n-empty" :data-description="description"><slot name="icon" /><slot name="extra" /></div>'
    },
    Button: {
      props: ['type', 'ghost', 'size'],
      template: '<button class="n-button" :data-type="type" @click="$emit(\'click\')"><slot /></button>'
    },
    SvgIcon: {
      props: ['localIcon'],
      template: '<i class="svg-icon" :data-icon="localIcon" />'
    }
  }
};

describe('ErrorState', () => {
  it('falls back to the generic state.error labels when no code/title supplied', () => {
    const wrapper = mount(ErrorState, { global });
    expect(wrapper.find('.n-empty').attributes('data-description')).toBe('page.state.error.title');
  });

  it('prefers an explicit title prop over any i18n fallback', () => {
    const wrapper = mount(ErrorState, { global, props: { title: 'boom' } });
    expect(wrapper.find('.n-empty').attributes('data-description')).toBe('boom');
  });

  it('emits retry when the refresh button is clicked', async () => {
    const wrapper = mount(ErrorState, { global });
    await wrapper.find('button.n-button').trigger('click');
    expect(wrapper.emitted('retry')?.length).toBeGreaterThanOrEqual(1);
  });

  it('hides the refresh button when retryable is false', () => {
    const wrapper = mount(ErrorState, { global, props: { retryable: false } });
    expect(wrapper.find('button.n-button').exists()).toBe(false);
  });

  it('renders an action slot for custom controls', () => {
    const wrapper = mount(ErrorState, {
      global,
      slots: { action: '<button class="cta">contact</button>' }
    });
    expect(wrapper.find('.cta').exists()).toBe(true);
  });
});
