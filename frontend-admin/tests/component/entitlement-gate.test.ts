import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { reactive, ref } from 'vue';

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

const authState = reactive({
  currentOrgId: '',
  userInfo: { roles: [] as string[] },
  decision: true
});

vi.mock('@/store/modules/auth', () => ({
  useAuthStore: () => ({
    canAccess: () => authState.decision,
    currentOrgId: authState.currentOrgId,
    userInfo: authState.userInfo
  })
}));

const pushSpy = vi.fn();

vi.mock('vue-router', () => ({
  useRoute: () => ({ meta: ref({}).value }),
  useRouter: () => ({ push: pushSpy })
}));

import EntitlementGate from '@/components/access/EntitlementGate.vue';

const stubs = {
  NResult: {
    props: ['status', 'title', 'description'],
    template:
      '<div class="n-result" :data-status="status" :data-title="title" :data-description="description"><slot name="footer" /></div>'
  },
  NSpace: { template: '<div class="n-space"><slot /></div>' },
  NButton: {
    template: '<button class="n-button" @click="$emit(\'click\')"><slot /></button>'
  }
};

describe('EntitlementGate', () => {
  it('renders the default slot when access is allowed', () => {
    authState.decision = true;
    authState.currentOrgId = 'org_1';
    authState.userInfo.roles = ['ORG_MEMBER'];
    const wrapper = mount(EntitlementGate, {
      global: { stubs },
      slots: { default: '<div class="ok">child</div>' }
    });
    expect(wrapper.find('.ok').exists()).toBe(true);
    expect(wrapper.find('.n-result').exists()).toBe(false);
  });

  it('renders the upgrade fallback when access is denied with default fallback', () => {
    authState.decision = false;
    authState.currentOrgId = 'org_1';
    authState.userInfo.roles = ['ORG_MEMBER'];
    const wrapper = mount(EntitlementGate, { global: { stubs } });
    expect(wrapper.find('.n-result').attributes('data-title')).toBe('page.accessGate.upgrade.title');
    expect(wrapper.find('.n-result').attributes('data-status')).toBe('info');
  });

  it('forces forbidden fallback when orgRequired but no current org', () => {
    authState.decision = false;
    authState.currentOrgId = '';
    const wrapper = mount(EntitlementGate, { global: { stubs }, props: { orgRequired: true } });
    expect(wrapper.find('.n-result').attributes('data-title')).toBe('page.accessGate.forbidden.title');
    expect(wrapper.find('.n-result').attributes('data-status')).toBe('403');
  });

  it('forces forbidden fallback when a required role is missing', () => {
    authState.decision = false;
    authState.currentOrgId = 'org_1';
    authState.userInfo.roles = ['ORG_MEMBER'];
    const wrapper = mount(EntitlementGate, { global: { stubs }, props: { role: 'ORG_OWNER' } });
    expect(wrapper.find('.n-result').attributes('data-title')).toBe('page.accessGate.forbidden.title');
  });

  it('routes the primary action to the configured destination', async () => {
    pushSpy.mockClear();
    authState.decision = false;
    authState.currentOrgId = 'org_1';
    const wrapper = mount(EntitlementGate, {
      global: { stubs },
      props: { fallback: 'contact-sales' }
    });
    await wrapper.findAll('button.n-button')[0].trigger('click');
    expect(pushSpy).toHaveBeenCalledWith('/admin/billing');
  });
});
