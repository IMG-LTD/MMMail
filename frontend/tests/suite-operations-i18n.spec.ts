import { describe, expect, it } from 'vitest'
import type {
  SuiteGovernanceChangeRequest,
  SuiteGovernanceOverview,
  SuiteRemediationExecutionResult,
  SuiteRiskLevel
} from '../types/api'
import { messages } from '../locales'
import { translate } from '../utils/i18n'
import {
  buildGovernanceOverviewCards,
  commandItemTypeLabel,
  governanceSlaLabel,
  governanceStatusLabel,
  reviewStageLabel,
  riskLevelLabel,
  summarizeExecution
} from '../utils/suite-operations'

const tEn = (key: string, params?: Record<string, string | number>) =>
  translate(messages, 'en', key, params)
const tZhCN = (key: string, params?: Record<string, string | number>) =>
  translate(messages, 'zh-CN', key, params)
const tZhTW = (key: string, params?: Record<string, string | number>) =>
  translate(messages, 'zh-TW', key, params)

describe('suite operations i18n helpers', () => {
  it('returns translated governance overview labels instead of locale keys', () => {
    const overview: SuiteGovernanceOverview = {
      generatedAt: '2026-04-03T08:00:00Z',
      totalRequests: 8,
      pendingReviewCount: 2,
      pendingSecondReviewCount: 1,
      approvedPendingExecutionCount: 1,
      rejectedCount: 1,
      executedCount: 2,
      executedWithFailureCount: 0,
      rolledBackCount: 1,
      rollbackWithFailureCount: 0,
      slaBreachedCount: 1
    }

    const cards = buildGovernanceOverviewCards(overview, tZhCN)

    expect(cards).toHaveLength(8)
    expect(cards[0]?.label).toBe('总请求数')
    expect(cards[4]?.label).toBe('已执行')
    expect(cards.every((card) => !card.label.startsWith('suite.operations.'))).toBe(true)
  })

  it('translates risk, status, review stage, sla, and command item labels', () => {
    const request: SuiteGovernanceChangeRequest = {
      requestId: 'req-1',
      orgId: 'org-1',
      ownerId: 'owner-1',
      templateCode: 'MAIL_BLOCK',
      templateName: 'Mail block',
      status: 'PENDING_SECOND_REVIEW',
      reason: 'rotate domains',
      requireDualReview: true,
      reviewStage: 'SECOND_REVIEW_PENDING',
      firstReviewNote: null,
      firstReviewedAt: null,
      firstReviewedByUserId: null,
      firstReviewedBySessionId: null,
      secondReviewerUserId: null,
      reviewNote: null,
      approvalNote: null,
      rollbackReason: null,
      requestedAt: '2026-04-03T08:00:00Z',
      reviewDueAt: '2026-04-03T10:00:00Z',
      reviewSlaBreached: true,
      reviewedAt: null,
      reviewedByUserId: null,
      reviewedBySessionId: null,
      approvedAt: null,
      executedAt: null,
      executedByUserId: null,
      executedBySessionId: null,
      rolledBackAt: null,
      actionCodes: [],
      rollbackActionCodes: [],
      executionResults: [],
      rollbackResults: []
    }

    expect(riskLevelLabel('CRITICAL' as SuiteRiskLevel, tZhCN)).toBe('严重')
    expect(governanceStatusLabel('PENDING_SECOND_REVIEW', tEn)).toBe('Pending second review')
    expect(reviewStageLabel('SECOND_REVIEW_PENDING', tZhTW)).toBe('待二審')
    expect(governanceSlaLabel(request, tZhTW)).toBe('已超時')
    expect(commandItemTypeLabel('VAULT_ITEM', tZhTW)).toBe('保險庫項目')
  })

  it('summarizes remediation execution with locale-aware labels', () => {
    const results: SuiteRemediationExecutionResult[] = [
      {
        actionCode: 'MAIL_FIX',
        productCode: 'MAIL',
        status: 'SUCCESS',
        message: 'done',
        executedAt: '2026-04-03T08:10:00Z',
        details: {}
      },
      {
        actionCode: 'MAIL_FIX',
        productCode: 'MAIL',
        status: 'NO_OP',
        message: 'noop',
        executedAt: '2026-04-03T08:11:00Z',
        details: {}
      },
      {
        actionCode: 'MAIL_FIX',
        productCode: 'MAIL',
        status: 'FAILED',
        message: 'failed',
        executedAt: '2026-04-03T08:12:00Z',
        details: {}
      }
    ]

    expect(summarizeExecution(results, tEn)).toBe('Success 1 / No changes needed 1 / Failed 1')
    expect(summarizeExecution(results, tZhCN)).toBe('成功 1 / 无需变更 1 / 失败 1')
  })
})
