import { ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'

export function useSheetsWorkspaceDialogs() {
  const { t } = useI18n()

  async function requestWorkbookTitle(mode: 'create' | 'rename', initialValue: string): Promise<string | null> {
    return requestTextInput({
      title: t(mode === 'create' ? 'sheets.prompts.createWorkbookTitle' : 'sheets.prompts.renameWorkbookTitle'),
      message: t(mode === 'create' ? 'sheets.prompts.createWorkbookMessage' : 'sheets.prompts.renameWorkbookMessage'),
      initialValue,
      confirmButtonText: t(mode === 'create' ? 'common.actions.create' : 'common.actions.rename'),
      inputErrorMessage: t('sheets.prompts.workbookTitleRequired')
    })
  }

  async function requestSheetName(initialValue: string): Promise<string | null> {
    return requestTextInput({
      title: t('sheets.prompts.renameSheetTitle'),
      message: t('sheets.prompts.renameSheetMessage'),
      initialValue,
      confirmButtonText: t('common.actions.rename'),
      inputErrorMessage: t('sheets.prompts.sheetNameRequired')
    })
  }

  async function confirmDeleteWorkbook(title: string): Promise<boolean> {
    return confirmAction({
      title: t('sheets.prompts.deleteWorkbookTitle'),
      message: t('sheets.prompts.deleteWorkbookMessage', { value: title }),
      confirmButtonText: t('common.actions.delete')
    })
  }

  async function confirmDeleteSheet(name: string): Promise<boolean> {
    return confirmAction({
      title: t('sheets.prompts.deleteSheetTitle'),
      message: t('sheets.prompts.deleteSheetMessage', { value: name }),
      confirmButtonText: t('common.actions.delete')
    })
  }

  async function confirmDiscardChanges(name: string): Promise<boolean> {
    return confirmAction({
      title: t('sheets.prompts.discardChangesTitle'),
      message: t('sheets.prompts.discardChangesMessage', { value: name }),
      confirmButtonText: t('common.actions.discard')
    })
  }

  function buildDefaultWorkbookTitle(): string {
    return t('sheets.prompts.defaultWorkbookTitle', { value: new Date().toLocaleString() })
  }

  function resolveErrorMessage(error: unknown, fallbackKey: string): string {
    return (error as Error)?.message || t(fallbackKey)
  }

  async function requestTextInput(options: {
    title: string
    message: string
    initialValue: string
    confirmButtonText: string
    inputErrorMessage: string
  }): Promise<string | null> {
    try {
      const prompt = await ElMessageBox.prompt(options.message, options.title, {
        confirmButtonText: options.confirmButtonText,
        cancelButtonText: t('common.actions.cancel'),
        inputValue: options.initialValue,
        inputPattern: /\S+/,
        inputErrorMessage: options.inputErrorMessage
      })
      return prompt.value.trim()
    } catch {
      return null
    }
  }

  async function confirmAction(options: {
    title: string
    message: string
    confirmButtonText: string
  }): Promise<boolean> {
    try {
      await ElMessageBox.confirm(options.message, options.title, {
        type: 'warning',
        confirmButtonText: options.confirmButtonText,
        cancelButtonText: t('common.actions.cancel')
      })
      return true
    } catch {
      return false
    }
  }

  return {
    requestWorkbookTitle,
    requestSheetName,
    confirmDeleteWorkbook,
    confirmDeleteSheet,
    confirmDiscardChanges,
    buildDefaultWorkbookTitle,
    resolveErrorMessage
  }
}
