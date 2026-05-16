import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

import OverviewStats from '@/views/business-overview/modules/overview-stats.vue';

const baseData = {
  orgId: 'org_1',
  orgName: 'MMMail',
  orgCode: 'mmmail',
  currentRole: 'ORG_ADMIN',
  memberCount: 12,
  adminCount: 3,
  pendingInviteCount: 2,
  teamSpaceCount: 4,
  storageBytes: 512 * 1024 * 1024,
  storageLimitBytes: 1024 * 1024 * 1024,
  allowedEmailDomains: ['mmmail.test'],
  governanceReviewSlaHours: 24,
  requireDualReviewGovernance: true,
  generatedAt: '2026-05-16T00:00:00Z'
};

describe('OverviewStats', () => {
  it('renders the card title and current role tag from data', () => {
    const wrapper = mount(OverviewStats, { props: { data: baseData } });
    expect(wrapper.text()).toContain('page.businessOverview.statsTitle');
    expect(wrapper.text()).toContain('ORG_ADMIN');
  });

  it('renders all six statistic labels when data is supplied', () => {
    const wrapper = mount(OverviewStats, { props: { data: baseData } });
    const text = wrapper.text();
    expect(text).toContain('page.businessOverview.memberCount');
    expect(text).toContain('page.businessOverview.adminCount');
    expect(text).toContain('page.businessOverview.pendingInviteCount');
    expect(text).toContain('page.businessOverview.teamSpaceCount');
    expect(text).toContain('page.businessOverview.storage');
    expect(text).toContain('page.businessOverview.governanceSla');
  });

  it('shows the dual-review badge when governance requires it', () => {
    const wrapper = mount(OverviewStats, { props: { data: baseData } });
    expect(wrapper.text()).toContain('page.businessOverview.dualReview');
  });

  it('hides the dual-review badge when governance allows single review', () => {
    const wrapper = mount(OverviewStats, {
      props: { data: { ...baseData, requireDualReviewGovernance: false } }
    });
    expect(wrapper.text()).not.toContain('page.businessOverview.dualReview');
  });

  it('renders 0% storage ratio when limit is zero to avoid divide-by-zero', () => {
    const wrapper = mount(OverviewStats, {
      props: { data: { ...baseData, storageBytes: 999, storageLimitBytes: 0 } }
    });
    expect(wrapper.text()).toContain('(0%)');
  });

  it('renders nothing inside the grid when data is null', () => {
    const wrapper = mount(OverviewStats, { props: { data: null } });
    expect(wrapper.text()).not.toContain('page.businessOverview.memberCount');
  });
});
