import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import PageStateWrapper from '@/components/feedback/PageStateWrapper.vue';

const stubs = {
  LoadingState: {
    props: ['mode', 'compact'],
    template: '<div class="loading-state" :data-mode="mode" :data-compact="String(compact)" />'
  },
  ErrorState: {
    props: ['code', 'retryable', 'compact'],
    emits: ['retry'],
    template:
      '<div class="error-state" :data-code="String(code)" :data-retryable="String(retryable)" @click="$emit(\'retry\')"><slot name="action" /></div>'
  },
  EmptyState: {
    props: ['description', 'icon', 'compact'],
    template: '<div class="empty-state" :data-description="description" :data-icon="icon"><slot name="action" /></div>'
  }
};

describe('PageStateWrapper', () => {
  it('renders LoadingState when loading is true and skips later branches', () => {
    const wrapper = mount(PageStateWrapper, {
      global: { stubs },
      props: { loading: true, error: 'oops', empty: true, loadingMode: 'skeleton', compact: true },
      slots: { default: '<div class="real">content</div>' }
    });
    expect(wrapper.find('.loading-state').exists()).toBe(true);
    expect(wrapper.find('.loading-state').attributes('data-mode')).toBe('skeleton');
    expect(wrapper.find('.loading-state').attributes('data-compact')).toBe('true');
    expect(wrapper.find('.error-state').exists()).toBe(false);
    expect(wrapper.find('.empty-state').exists()).toBe(false);
    expect(wrapper.find('.real').exists()).toBe(false);
  });

  it('renders ErrorState when error is truthy and forwards errorCode + retry', async () => {
    const wrapper = mount(PageStateWrapper, {
      global: { stubs },
      props: { error: new Error('boom'), errorCode: 30001, retryable: false },
      slots: {
        default: '<div class="real" />',
        errorAction: '<button class="alt-action">alt</button>'
      }
    });
    const error = wrapper.find('.error-state');
    expect(error.exists()).toBe(true);
    expect(error.attributes('data-code')).toBe('30001');
    expect(error.attributes('data-retryable')).toBe('false');
    expect(wrapper.find('.alt-action').exists()).toBe(true);
    expect(wrapper.find('.real').exists()).toBe(false);

    await error.trigger('click');
    expect(wrapper.emitted('retry')).toHaveLength(1);
  });

  it('renders EmptyState when empty is true and no error/loading', () => {
    const wrapper = mount(PageStateWrapper, {
      global: { stubs },
      props: { empty: true, emptyDescription: 'nothing here', emptyIcon: 'not-found', compact: true },
      slots: {
        default: '<div class="real" />',
        emptyAction: '<button class="empty-cta">add</button>'
      }
    });
    const empty = wrapper.find('.empty-state');
    expect(empty.exists()).toBe(true);
    expect(empty.attributes('data-description')).toBe('nothing here');
    expect(empty.attributes('data-icon')).toBe('not-found');
    expect(wrapper.find('.empty-cta').exists()).toBe(true);
    expect(wrapper.find('.real').exists()).toBe(false);
  });

  it('renders default slot when no state flags are set', () => {
    const wrapper = mount(PageStateWrapper, {
      global: { stubs },
      slots: { default: '<div class="real">content</div>' }
    });
    expect(wrapper.find('.real').exists()).toBe(true);
    expect(wrapper.find('.loading-state').exists()).toBe(false);
    expect(wrapper.find('.error-state').exists()).toBe(false);
    expect(wrapper.find('.empty-state').exists()).toBe(false);
  });
});
