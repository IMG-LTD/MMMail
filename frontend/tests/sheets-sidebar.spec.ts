import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import SheetsWorkbookSidebar from '../components/sheets/SheetsWorkbookSidebar.vue'
import type { SheetsWorkbookSummary } from '../types/sheets'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce(
        (result, [paramKey, value]) => result.replace(`{${paramKey}}`, String(value)),
        key
      )
    }
  })
}))

const ElButton = defineComponent({
  name: 'ElButton',
  props: {
    disabled: { type: Boolean, default: false }
  },
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' }
  },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)">'
})

const ElSkeleton = defineComponent({
  name: 'ElSkeleton',
  template: '<div v-bind="$attrs" class="el-skeleton-stub" />'
})

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Roadmap workbook',
    rowCount: 8,
    colCount: 4,
    filledCellCount: 12,
    formulaCellCount: 2,
    computedErrorCount: 0,
    currentVersion: 3,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T08:00:00',
    lastOpenedAt: '2026-03-30T08:05:00',
    permission: 'OWNER',
    scope: 'OWNED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 2,
    canEdit: true,
    ...overrides
  }
}

function mountSidebar(workbooks: SheetsWorkbookSummary[]) {
  return mount(SheetsWorkbookSidebar, {
    props: {
      workbooks,
      activeWorkbookId: null,
      loading: false,
      busyWorkbookId: null
    },
    global: {
      components: {
        ElButton,
        ElInput,
        ElSkeleton
      }
    }
  })
}

describe('SheetsWorkbookSidebar', () => {
  it('filters workbooks by title and owner keyword', async () => {
    const wrapper = mountSidebar([
      buildWorkbookSummary({ id: 'wb-1', title: 'Roadmap workbook', ownerEmail: 'owner@mmmail.local' }),
      buildWorkbookSummary({ id: 'wb-2', title: 'Quarter budget', ownerEmail: 'finance@mmmail.local', ownerDisplayName: 'Finance', scope: 'SHARED', permission: 'EDIT', canEdit: true }),
      buildWorkbookSummary({ id: 'wb-3', title: 'Hiring plan', ownerEmail: 'people@mmmail.local', ownerDisplayName: 'People Ops' })
    ])

    await wrapper.get('[data-testid="sheets-sidebar-search"]').setValue('finance')

    expect(wrapper.find('[data-testid="sheets-workbook-wb-1"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="sheets-workbook-wb-2"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-workbook-wb-3"]').exists()).toBe(false)

    await wrapper.get('[data-testid="sheets-sidebar-search"]').setValue('')

    expect(wrapper.find('[data-testid="sheets-workbook-wb-1"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-workbook-wb-2"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-workbook-wb-3"]').exists()).toBe(true)
  })

  it('shows filter empty state when no workbook matches', async () => {
    const wrapper = mountSidebar([
      buildWorkbookSummary({ id: 'wb-1', title: 'Roadmap workbook' }),
      buildWorkbookSummary({ id: 'wb-2', title: 'Quarter budget' })
    ])

    await wrapper.get('[data-testid="sheets-sidebar-search"]').setValue('legal')

    expect(wrapper.find('[data-testid="sheets-sidebar-filter-empty"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="sheets-sidebar-filter-empty"]').text()).toContain('sheets.sidebar.filteredEmptyTitle')
    expect(wrapper.find('[data-testid="sheets-workbook-wb-1"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="sheets-workbook-wb-2"]').exists()).toBe(false)
  })

  it('keeps select and owner actions wired after filtering', async () => {
    const wrapper = mountSidebar([
      buildWorkbookSummary({ id: 'wb-1', title: 'Roadmap workbook' }),
      buildWorkbookSummary({ id: 'wb-2', title: 'Quarter budget' })
    ])

    await wrapper.get('[data-testid="sheets-sidebar-search"]').setValue('road')
    await wrapper.get('[data-testid="sheets-workbook-wb-1"]').trigger('click')
    await wrapper.get('[data-testid="sheets-workbook-rename-wb-1"]').trigger('click')
    await wrapper.get('[data-testid="sheets-workbook-delete-wb-1"]').trigger('click')

    expect(wrapper.emitted('select')).toEqual([['wb-1']])
    expect(wrapper.emitted('rename')).toEqual([[expect.objectContaining({ id: 'wb-1' })]])
    expect(wrapper.emitted('delete')).toEqual([[expect.objectContaining({ id: 'wb-1' })]])
  })
})
