import { defineComponent, h, inject, provide, ref, toRef } from 'vue'
import { mount, type VueWrapper } from '@vue/test-utils'

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div v-bind="$attrs" class="el-alert-stub"><slot />{{ title }}</div>',
})

const ElButton = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: [String, Number], default: '' },
    type: { type: String, default: 'text' },
  },
  emits: ['update:modelValue', 'keyup'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      v-bind="$attrs"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup="$emit('keyup', $event)"
    />
    <input
      v-else
      v-bind="$attrs"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup="$emit('keyup', $event)"
    />
  `,
})

const ElOption = defineComponent({
  name: 'ElOption',
  props: {
    label: { type: String, default: '' },
    value: { type: [String, Number], default: '' },
  },
  template: '<option :value="value">{{ label || value }}</option>',
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: {
    modelValue: { type: [String, Number, Array], default: '' },
    multiple: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  methods: {
    onChange(event: Event) {
      const target = event.target as HTMLSelectElement
      if (this.multiple) {
        const values = Array.from(target.selectedOptions).map((option) => option.value)
        this.$emit('update:modelValue', values)
        return
      }
      this.$emit('update:modelValue', target.value)
    },
  },
  template: '<select v-bind="$attrs" :multiple="multiple" @change="onChange"><slot /></select>',
})

const ElDialog = defineComponent({
  name: 'ElDialog',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-dialog-stub"><slot /><slot name="footer" /></div>',
})

const ElDrawer = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-drawer-stub"><slot /></div>',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

const ElProgress = defineComponent({
  name: 'ElProgress',
  props: { percentage: { type: Number, default: 0 } },
  template: '<div class="el-progress-stub">{{ percentage }}</div>',
})

const ElBreadcrumb = defineComponent({
  name: 'ElBreadcrumb',
  template: '<nav><slot /></nav>',
})

const ElBreadcrumbItem = defineComponent({
  name: 'ElBreadcrumbItem',
  template: '<span><slot /></span>',
})

const ElSegmented = defineComponent({
  name: 'ElSegmented',
  props: {
    modelValue: { type: String, default: '' },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  template: `
    <div class="el-segmented-stub">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        :data-testid="\`drive-view-\${String(option.value).toLowerCase()}\`"
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `,
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div class="el-empty-stub">{{ description }}</div>',
})

const ElInputNumber = defineComponent({
  name: 'ElInputNumber',
  props: { modelValue: { type: Number, default: 0 } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))">',
})

const TABLE_ROWS_KEY = Symbol('drive-table-rows')
const TABLE_SELECTION_KEY = Symbol('drive-table-selection')

const ElTable = defineComponent({
  name: 'ElTable',
  props: {
    data: { type: Array, default: () => [] },
  },
  emits: ['selection-change'],
  setup(props, { emit, slots, expose }) {
    const rows = toRef(props, 'data')
    const selectedRows = ref<Record<string, unknown>[]>([])
    function updateSelection(row: Record<string, unknown>, checked: boolean): void {
      const rowId = String(row.id ?? '')
      selectedRows.value = checked
        ? [...selectedRows.value.filter((item) => String(item.id ?? '') !== rowId), row]
        : selectedRows.value.filter((item) => String(item.id ?? '') !== rowId)
      emit('selection-change', selectedRows.value)
    }
    function clearSelection(): void {
      selectedRows.value = []
      emit('selection-change', [])
    }
    provide(TABLE_ROWS_KEY, rows)
    provide(TABLE_SELECTION_KEY, updateSelection)
    expose({ clearSelection })
    return () => h('div', { class: 'el-table-stub' }, slots.default ? slots.default() : [])
  },
})

const ElTableColumn = defineComponent({
  name: 'ElTableColumn',
  props: {
    type: { type: String, default: '' },
    prop: { type: String, default: '' },
  },
  setup(props, { slots }) {
    const rows = inject(TABLE_ROWS_KEY, ref<Record<string, unknown>[]>([]))
    const updateSelection = inject<(row: Record<string, unknown>, checked: boolean) => void>(
      TABLE_SELECTION_KEY,
      () => undefined,
    )
    return () => h(
      'div',
      { class: 'el-table-column-stub' },
      rows.value.map((row, index) => {
        if (props.type === 'selection') {
          return h('label', { key: `selection-${String(row.id ?? index)}` }, [
            h('input', {
              type: 'checkbox',
              'data-testid': `table-select-${String(row.id ?? index)}`,
              onChange: (event: Event) => updateSelection(row, (event.target as HTMLInputElement).checked),
            }),
          ])
        }
        const content = slots.default
          ? slots.default({ row, $index: index })
          : [h('span', row[props.prop as keyof typeof row] as string || '')]
        return h('div', { key: `cell-${String(row.id ?? index)}-${props.prop || index}` }, content)
      }),
    )
  },
})

const DriveCollaborationLaunchpadStub = defineComponent({
  name: 'DriveCollaborationLaunchpad',
  template: '<div data-testid="drive-launchpad-stub"></div>',
})

const DriveIncomingSharesPanelStub = defineComponent({
  name: 'DriveIncomingSharesPanel',
  template: '<div data-testid="drive-incoming-stub"></div>',
})

const DriveCollaboratorSharedPanelStub = defineComponent({
  name: 'DriveCollaboratorSharedPanel',
  template: '<div data-testid="drive-collaborator-shared-stub"></div>',
})

const DriveSharedWithMePanelStub = defineComponent({
  name: 'DriveSharedWithMePanel',
  template: '<div data-testid="drive-shared-with-me-stub"></div>',
})

const DriveShareLinksDrawerStub = defineComponent({
  name: 'DriveShareLinksDrawer',
  props: {
    modelValue: { type: Boolean, default: false },
    item: { type: Object, default: null },
  },
  template: '<div v-if="modelValue" data-testid="drive-share-drawer-stub">{{ item?.id }}</div>',
})

const DriveBatchShareDialogStub = defineComponent({
  name: 'DriveBatchShareDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    items: { type: Array, default: () => [] },
  },
  emits: ['created'],
  template: `
    <div v-if="modelValue" data-testid="drive-batch-share-dialog-stub">
      {{ items.length }}
      <button data-testid="drive-batch-share-created" type="button" @click="$emit('created')">created</button>
    </div>
  `,
})

export async function mountDrivePage() {
  const { default: DrivePage } = await import('~/pages/drive.vue')
  return mount(DrivePage, {
    global: {
      stubs: {
        ElAlert,
        ElBreadcrumb,
        ElBreadcrumbItem,
        ElButton,
        ElDialog,
        ElDrawer,
        ElEmpty,
        ElInput,
        ElInputNumber,
        ElOption,
        ElProgress,
        ElSegmented,
        ElSelect,
        ElTable,
        ElTableColumn,
        ElTag,
        DriveBatchShareDialog: DriveBatchShareDialogStub,
        DriveCollaborationLaunchpad: DriveCollaborationLaunchpadStub,
        DriveCollaboratorSharedPanel: DriveCollaboratorSharedPanelStub,
        DriveIncomingSharesPanel: DriveIncomingSharesPanelStub,
        DriveShareLinksDrawer: DriveShareLinksDrawerStub,
        DriveSharedWithMePanel: DriveSharedWithMePanelStub,
      },
      directives: {
        loading: () => undefined,
      },
    },
  })
}

export async function attachFile(
  wrapper: VueWrapper,
  selector: string,
  fileName: string,
  content: string,
): Promise<void> {
  const input = wrapper.get(selector).element as HTMLInputElement
  const file = new File([content], fileName, { type: 'text/plain' })
  Object.defineProperty(input, 'files', {
    configurable: true,
    value: [file],
  })
  await wrapper.get(selector).trigger('change')
}
