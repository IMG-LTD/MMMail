import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

import EmptyState from '@/components/feedback/EmptyState.vue';

const global = {
  stubs: {
    Empty: {
      props: ['description'],
      template: '<div class="n-empty" :data-description="description"><slot name="icon" /><slot name="extra" /></div>'
    },
    SvgIcon: {
      props: ['localIcon'],
      template: '<i class="svg-icon" :data-icon="localIcon" />'
    }
  }
};

describe('EmptyState', () => {
  it('falls back to page.state.empty.description when no description prop is supplied', () => {
    const wrapper = mount(EmptyState, { global });
    expect(wrapper.find('.n-empty').attributes('data-description')).toBe('page.state.empty.description');
    expect(wrapper.attributes('role')).toBe('status');
    expect(wrapper.attributes('aria-live')).toBe('polite');
  });

  it('uses the description prop when supplied', () => {
    const wrapper = mount(EmptyState, { global, props: { description: 'no inbox yet' } });
    expect(wrapper.find('.n-empty').attributes('data-description')).toBe('no inbox yet');
  });

  it('switches to compact paddings when compact is true', () => {
    const wrapper = mount(EmptyState, { global, props: { compact: true } });
    expect(wrapper.classes()).toContain('py-16px');
    expect(wrapper.classes()).not.toContain('py-48px');
  });

  it('renders a custom icon and action slot', () => {
    const wrapper = mount(EmptyState, {
      global,
      props: { icon: 'not-found' },
      slots: { action: '<button class="cta">retry</button>' }
    });
    expect(wrapper.find('.svg-icon').attributes('data-icon')).toBe('not-found');
    expect(wrapper.find('.cta').exists()).toBe(true);
  });
});
