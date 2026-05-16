import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

import LoadingState from '@/components/feedback/LoadingState.vue';

const global = {
  stubs: {
    Spin: {
      props: ['size'],
      template: '<div class="n-spin" :data-size="size" />'
    },
    Skeleton: {
      props: ['text', 'repeat', 'sharp'],
      template: '<div class="n-skeleton" :data-repeat="repeat" :data-text="String(text)" :data-sharp="String(sharp)" />'
    }
  }
};

describe('LoadingState', () => {
  it('renders a spin with default i18n description', () => {
    const wrapper = mount(LoadingState, { global });
    expect(wrapper.find('.n-spin').exists()).toBe(true);
    expect(wrapper.find('.n-spin').attributes('data-size')).toBe('medium');
    expect(wrapper.text()).toContain('page.state.loading.description');
    expect(wrapper.attributes('aria-busy')).toBe('true');
  });

  it('uses the description prop when supplied', () => {
    const wrapper = mount(LoadingState, { global, props: { description: 'syncing inbox' } });
    expect(wrapper.text()).toContain('syncing inbox');
  });

  it('uses small spin in compact mode', () => {
    const wrapper = mount(LoadingState, { global, props: { compact: true } });
    expect(wrapper.find('.n-spin').attributes('data-size')).toBe('small');
    expect(wrapper.classes()).toContain('py-12px');
  });

  it('renders a skeleton when mode is skeleton', () => {
    const wrapper = mount(LoadingState, { global, props: { mode: 'skeleton', rows: 5 } });
    expect(wrapper.find('.n-spin').exists()).toBe(false);
    expect(wrapper.find('.n-skeleton').attributes('data-repeat')).toBe('5');
  });
});
