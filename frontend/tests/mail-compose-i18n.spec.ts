import { describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import MailAttachmentPanel from '../components/business/MailAttachmentPanel.vue'
import MailComposer from '../components/business/MailComposer.vue'
import { messages } from '../locales'
import { translate } from '../utils/i18n'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: { value: 'en' },
    t: (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)
  })
}))

const stubs = {
  ElButton: defineComponent({
    name: 'ElButton',
    template: '<button><slot /></button>'
  }),
  ElForm: defineComponent({
    name: 'ElForm',
    template: '<form><slot /></form>'
  }),
  ElFormItem: defineComponent({
    name: 'ElFormItem',
    props: { label: { type: String, default: '' } },
    template: '<label><span>{{ label }}</span><slot /></label>'
  }),
  ElInput: defineComponent({
    name: 'ElInput',
    props: { modelValue: { type: String, default: '' }, placeholder: { type: String, default: '' } },
    emits: ['update:modelValue'],
    template: '<input :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)">'
  }),
  ElAutocomplete: defineComponent({
    name: 'ElAutocomplete',
    props: { modelValue: { type: String, default: '' }, placeholder: { type: String, default: '' } },
    emits: ['update:modelValue'],
    template: '<input :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)">'
  }),
  ElSelect: defineComponent({
    name: 'ElSelect',
    template: '<div><slot /></div>'
  }),
  ElOption: defineComponent({
    name: 'ElOption',
    template: '<option><slot /></option>'
  }),
  ElDatePicker: defineComponent({
    name: 'ElDatePicker',
    props: { placeholder: { type: String, default: '' } },
    template: '<input :placeholder="placeholder">'
  }),
  ElSwitch: defineComponent({
    name: 'ElSwitch',
    props: { modelValue: { type: Boolean, default: false } },
    emits: ['update:modelValue'],
    template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)">'
  }),
  ElEmpty: defineComponent({
    name: 'ElEmpty',
    props: { description: { type: String, default: '' } },
    template: '<div class="el-empty">{{ description }}</div>'
  }),
  ElAlert: defineComponent({
    name: 'ElAlert',
    props: {
      title: { type: String, default: '' },
      description: { type: String, default: '' }
    },
    template: '<div class="el-alert">{{ title }} {{ description }}<slot /></div>'
  })
}

describe('mail compose i18n', () => {
  it('renders localized composer labels and hint', () => {
    const wrapper = mount(MailComposer, {
      props: {
        autoSaveSeconds: 15,
        recipientE2eeStatus: {
          toEmail: 'alice@example.com',
          fromEmail: 'owner@mmmail.local',
          deliverable: true,
          encryptionReady: true,
          readiness: 'READY',
          routeCount: 1,
          routes: [{
            targetEmail: 'alice@example.com',
            forwardToEmail: 'alice@example.com',
            keyAvailable: true,
            fingerprint: 'ABCD',
            algorithm: 'curve25519Legacy',
            publicKeyArmored: 'PUBLIC_KEY'
          }]
        },
        senderOptions: [{
          identityId: null,
          orgId: null,
          orgName: null,
          memberId: null,
          emailAddress: 'owner@mmmail.local',
          displayName: 'Owner',
          source: 'PRIMARY',
          status: 'ENABLED',
          defaultIdentity: true
        }]
      },
      global: { stubs }
    })

    expect(wrapper.text()).toContain('Compose')
    expect(wrapper.text()).toContain('From')
    expect(wrapper.text()).toContain('Schedule Send (Optional)')
    expect(wrapper.text()).toContain('Send')
    expect(wrapper.text()).toContain('Save Draft')
    expect(wrapper.text()).toContain('Auto-save is enabled every 15 seconds.')
    expect(wrapper.text()).toContain('Recipient route is encryption-ready')
    expect(wrapper.text()).toContain('the message body is encrypted automatically on send.')
  })

  it('renders localized attachment panel states', () => {
    const wrapper = mount(MailAttachmentPanel, {
      props: {
        failedUploads: [{ id: 'f-1', fileName: 'blocked.exe', message: 'Attachment type is not allowed' }]
      },
      global: { stubs }
    })

    expect(wrapper.text()).toContain('Attachments')
    expect(wrapper.text()).toContain('20MB max per file. Executable files are blocked.')
    expect(wrapper.text()).toContain('Add files')
    expect(wrapper.text()).toContain('No attachments yet')
    expect(wrapper.text()).toContain('Retry')
  })

  it('renders external secure delivery copy for smtp outbound recipients', () => {
    const wrapper = mount(MailComposer, {
      props: {
        autoSaveSeconds: 15,
        recipientE2eeStatus: {
          toEmail: 'external@example.net',
          fromEmail: 'owner@mmmail.local',
          deliverable: true,
          encryptionReady: false,
          readiness: 'NOT_READY',
          routeCount: 1,
          routes: [{
            targetEmail: 'external@example.net',
            forwardToEmail: 'external@example.net',
            keyAvailable: false,
            fingerprint: null,
            algorithm: null,
            publicKeyArmored: null,
            smtpOutbound: true
          }]
        },
        senderOptions: [{
          identityId: null,
          orgId: null,
          orgName: null,
          memberId: null,
          emailAddress: 'owner@mmmail.local',
          displayName: 'Owner',
          source: 'PRIMARY',
          status: 'ENABLED',
          defaultIdentity: true
        }]
      },
      global: { stubs }
    })

    expect(wrapper.get('[data-testid="mail-compose-external-secure"]').text()).toContain('Password-protected secure delivery')
    expect(wrapper.text()).toContain('Encrypt the message body in the browser, email a secure link through SMTP, and share the password out of band.')
  })
})
