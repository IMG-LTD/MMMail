import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick, ref } from 'vue';

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

const overviewMock = vi.fn();
const teamSpacesMock = vi.fn();

vi.mock('@/service/api/org-business', () => ({
  readOrgBusinessOverview: (...args: unknown[]) => overviewMock(...args),
  listOrgTeamSpaces: (...args: unknown[]) => teamSpacesMock(...args),
  listOrgTeamSpaceItems: vi.fn(),
  buildOrgTeamSpaceFileDownloadUrl: () => ''
}));

const orgIdRef = ref<string>('org-1');
vi.mock('@/store/modules/auth', () => ({
  useAuthStore: () => ({
    get currentOrgId() {
      return orgIdRef.value;
    }
  })
}));

vi.mock('@/components/access/EntitlementGate.vue', () => ({
  default: defineComponent({
    name: 'EntitlementGate',
    props: ['requires', 'orgRequired', 'fallback'],
    setup(_, { slots }) {
      return () => h('div', { class: 'entitlement-gate' }, slots.default?.());
    }
  })
}));

vi.mock('@/components/feedback', () => ({
  PageStateWrapper: defineComponent({
    name: 'PageStateWrapperStub',
    props: ['loading', 'error', 'empty', 'loadingMode'],
    emits: ['retry'],
    setup(props, { slots, emit }) {
      return () => {
        if (props.loading) return h('div', { class: 'state-loading' });
        if (props.error)
          return h(
            'div',
            { class: 'state-error', onClick: () => emit('retry') },
            String((props.error as Error)?.message ?? props.error)
          );
        if (props.empty) return h('div', { class: 'state-empty' });
        return h('div', { class: 'state-success' }, slots.default?.());
      };
    }
  })
}));

vi.mock('@/views/business-overview/modules/overview-stats.vue', () => ({
  default: defineComponent({
    name: 'OverviewStats',
    props: ['data'],
    setup(props) {
      return () => h('div', { class: 'overview-stats', 'data-org': props.data?.orgId ?? '' });
    }
  })
}));

vi.mock('@/views/business-overview/modules/team-spaces-table.vue', () => ({
  default: defineComponent({
    name: 'TeamSpacesTable',
    props: ['rows', 'loading'],
    emits: ['open'],
    setup(props, { emit }) {
      return () =>
        h(
          'div',
          { class: 'team-spaces-table', 'data-rows': String(props.rows.length) },
          h('button', { class: 'open-btn', onClick: () => emit('open', props.rows[0]) }, 'open')
        );
    }
  })
}));

vi.mock('@/views/business-overview/modules/team-space-drawer.vue', () => ({
  default: defineComponent({
    name: 'TeamSpaceDrawer',
    props: ['orgId', 'teamSpace', 'show'],
    setup(props) {
      return () =>
        h('div', {
          class: 'team-space-drawer',
          'data-show': String(props.show),
          'data-team-space': props.teamSpace?.id ?? ''
        });
    }
  })
}));

import BusinessOverview from '@/views/business-overview/index.vue';

const sampleOverview = {
  orgId: 'org-1',
  orgName: 'Acme',
  currentRole: 'OWNER',
  memberCount: 12,
  adminCount: 3,
  pendingInviteCount: 2,
  teamSpaceCount: 4,
  storageBytes: 1024,
  storageLimitBytes: 4096,
  allowedEmailDomains: [],
  governanceReviewSlaHours: 24,
  requireDualReviewGovernance: true,
  generatedAt: '2024-01-01T00:00:00'
};

const sampleTeamSpaces = [
  {
    id: 'ts-1',
    orgId: 'org-1',
    name: 'Engineering',
    slug: 'eng',
    description: '',
    rootItemId: 'root',
    storageBytes: 1,
    storageLimitBytes: 1024,
    itemCount: 5,
    createdBy: 'a@b.c',
    currentAccessRole: 'EDITOR',
    canWrite: true,
    canManage: false,
    updatedAt: '2024-01-01'
  }
];

beforeEach(() => {
  orgIdRef.value = 'org-1';
  overviewMock.mockReset();
  teamSpacesMock.mockReset();
});

afterEach(() => {
  vi.clearAllMocks();
});

async function flush() {
  await nextTick();
  await Promise.resolve();
  await Promise.resolve();
  await nextTick();
}

describe('BusinessOverview view', () => {
  it('renders loading state then success and forwards data to children', async () => {
    let resolveOverview: (v: unknown) => void = () => {};
    overviewMock.mockReturnValueOnce(
      new Promise(resolve => {
        resolveOverview = resolve;
      })
    );
    teamSpacesMock.mockResolvedValueOnce({ error: null, data: sampleTeamSpaces });

    const wrapper = mount(BusinessOverview);
    await nextTick();
    expect(wrapper.find('.state-loading').exists()).toBe(true);

    resolveOverview({ error: null, data: sampleOverview });
    await flush();

    expect(wrapper.find('.state-success').exists()).toBe(true);
    expect(wrapper.find('.overview-stats').attributes('data-org')).toBe('org-1');
    expect(wrapper.find('.team-spaces-table').attributes('data-rows')).toBe('1');
  });

  it('renders error state when any request fails and recovers on retry', async () => {
    overviewMock.mockResolvedValueOnce({ error: new Error('boom'), data: null });
    teamSpacesMock.mockResolvedValueOnce({ error: null, data: sampleTeamSpaces });

    const wrapper = mount(BusinessOverview);
    await flush();

    expect(wrapper.find('.state-error').exists()).toBe(true);
    expect(wrapper.find('.state-error').text()).toContain('boom');

    overviewMock.mockResolvedValueOnce({ error: null, data: sampleOverview });
    teamSpacesMock.mockResolvedValueOnce({ error: null, data: sampleTeamSpaces });
    await wrapper.find('.state-error').trigger('click');
    await flush();

    expect(wrapper.find('.state-success').exists()).toBe(true);
  });

  it('renders empty state when overview is null and no team spaces are returned', async () => {
    overviewMock.mockResolvedValueOnce({ error: null, data: null });
    teamSpacesMock.mockResolvedValueOnce({ error: null, data: [] });

    const wrapper = mount(BusinessOverview);
    await flush();

    expect(wrapper.find('.state-empty').exists()).toBe(true);
  });

  it('opens the drawer when the table emits open and forwards the team space', async () => {
    overviewMock.mockResolvedValueOnce({ error: null, data: sampleOverview });
    teamSpacesMock.mockResolvedValueOnce({ error: null, data: sampleTeamSpaces });

    const wrapper = mount(BusinessOverview);
    await flush();
    expect(wrapper.find('.team-space-drawer').attributes('data-show')).toBe('false');

    await wrapper.find('.open-btn').trigger('click');
    await nextTick();

    const drawer = wrapper.find('.team-space-drawer');
    expect(drawer.attributes('data-show')).toBe('true');
    expect(drawer.attributes('data-team-space')).toBe('ts-1');
  });

  it('skips loading when there is no current org', async () => {
    orgIdRef.value = '';
    const wrapper = mount(BusinessOverview);
    await flush();

    expect(overviewMock).not.toHaveBeenCalled();
    expect(teamSpacesMock).not.toHaveBeenCalled();
    expect(wrapper.find('.state-empty').exists()).toBe(true);
  });
});
