import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import ContactsWorkspace from '../components/contacts/ContactsWorkspace.vue'
import { messages } from '../locales'
import { translate } from '../utils/i18n'

const activeLocale = { value: 'en' as 'en' | 'zh-CN' }

const contactApiMock = {
  listContacts: vi.fn(),
  createContact: vi.fn(),
  updateContact: vi.fn(),
  deleteContact: vi.fn(),
  favoriteContact: vi.fn(),
  unfavoriteContact: vi.fn(),
  importContactsCsv: vi.fn(),
  exportContacts: vi.fn(),
  listDuplicateContacts: vi.fn(),
  mergeDuplicateContacts: vi.fn(),
  listGroups: vi.fn(),
  createGroup: vi.fn(),
  updateGroup: vi.fn(),
  deleteGroup: vi.fn(),
  listGroupMembers: vi.fn(),
  addGroupMembers: vi.fn(),
  removeGroupMember: vi.fn()
}

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: activeLocale,
    t: (key: string, params?: Record<string, string | number>) => {
      return translate(messages, activeLocale.value, key, params)
    }
  })
}))

vi.mock('~/composables/useContactApi', () => ({
  useContactApi: () => contactApiMock
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

vi.stubGlobal('useHead', vi.fn())

const stubs = {
  ElButton: defineComponent({
    name: 'ElButton',
    template: '<button><slot /></button>'
  }),
  ElInput: defineComponent({
    name: 'ElInput',
    props: {
      modelValue: { default: '' },
      placeholder: { type: String, default: '' },
      type: { type: String, default: 'text' },
      rows: { type: Number, default: 0 }
    },
    emits: ['update:modelValue'],
    template: '<input :value="modelValue" :placeholder="placeholder" :data-type="type" :data-rows="rows" @input="$emit(\'update:modelValue\', $event.target.value)">'
  }),
  ElSwitch: defineComponent({
    name: 'ElSwitch',
    props: { activeText: { type: String, default: '' } },
    template: '<div class="switch-stub">{{ activeText }}</div>'
  }),
  ElTag: defineComponent({
    name: 'ElTag',
    template: '<span><slot /></span>'
  }),
  ElTable: defineComponent({
    name: 'ElTable',
    props: { emptyText: { type: String, default: '' } },
    template: '<div><span class="empty-text">{{ emptyText }}</span><slot /></div>'
  }),
  ElTableColumn: defineComponent({
    name: 'ElTableColumn',
    props: { label: { type: String, default: '' } },
    setup() {
      return {
        row: {
          id: 'contact-1',
          isFavorite: false,
          displayName: 'Alice',
          email: 'alice@example.com',
          signature: 'alice@example.com',
          contacts: []
        }
      }
    },
    template: '<div><span>{{ label }}</span><slot :row="row" /></div>'
  }),
  ElSelect: defineComponent({
    name: 'ElSelect',
    props: { placeholder: { type: String, default: '' } },
    template: '<div><span>{{ placeholder }}</span><slot /></div>'
  }),
  ElOption: defineComponent({
    name: 'ElOption',
    props: { label: { type: String, default: '' } },
    template: '<option>{{ label }}</option>'
  }),
  ElDialog: defineComponent({
    name: 'ElDialog',
    props: { title: { type: String, default: '' } },
    template: '<div><h3>{{ title }}</h3><slot /><slot name="footer" /></div>'
  }),
  ElFormItem: defineComponent({
    name: 'ElFormItem',
    props: { label: { type: String, default: '' } },
    template: '<label><span>{{ label }}</span><slot /></label>'
  })
}

const mountOptions = {
  global: {
    stubs,
    directives: {
      loading: {
        mounted() {}
      }
    }
  }
}

describe('contacts workspace i18n', () => {
  beforeEach(() => {
    activeLocale.value = 'en'
    contactApiMock.listContacts.mockResolvedValue([])
    contactApiMock.listGroups.mockResolvedValue([])
    contactApiMock.listDuplicateContacts.mockResolvedValue([])
    contactApiMock.listGroupMembers.mockResolvedValue([])
  })

  it('renders english labels and placeholders', async () => {
    const wrapper = mount(ContactsWorkspace, mountOptions)

    await flushPromises()

    expect(wrapper.text()).toContain('Contacts')
    expect(wrapper.text()).toContain('Groups')
    expect(wrapper.text()).toContain('Import / Export')
    expect(wrapper.text()).toContain('Duplicates')
    expect(wrapper.text()).toContain('Create Contact')
    expect(wrapper.text()).toContain('Edit Contact')
    expect(wrapper.html()).toContain('Search by name or email')
    expect(wrapper.html()).toContain('Display name')
  })

  it('renders simplified chinese labels and placeholders', async () => {
    activeLocale.value = 'zh-CN'

    const wrapper = mount(ContactsWorkspace, mountOptions)

    await flushPromises()

    expect(wrapper.text()).toContain('联系人')
    expect(wrapper.text()).toContain('分组')
    expect(wrapper.text()).toContain('导入 / 导出')
    expect(wrapper.text()).toContain('重复联系人')
    expect(wrapper.text()).toContain('创建联系人')
    expect(wrapper.text()).toContain('编辑联系人')
    expect(wrapper.html()).toContain('按姓名或邮箱搜索')
    expect(wrapper.html()).toContain('显示名称')
  })
})
