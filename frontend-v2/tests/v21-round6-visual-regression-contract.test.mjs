import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  adminView: new URL('../src/views/app/AdminSectionView.vue', import.meta.url),
  calendarCss: new URL('../src/views/app/calendar-view.css', import.meta.url),
  collaborationView: new URL('../src/views/app/CollaborationView.vue', import.meta.url),
  commandCenterView: new URL('../src/views/app/CommandCenterView.vue', import.meta.url),
  dataTable: new URL('../src/design-system/components/DataTable.vue', import.meta.url),
  docsWorkspace: new URL('../src/views/app/DocsWorkspaceView.vue', import.meta.url),
  driveCss: new URL('../src/views/app/drive-section-view.css', import.meta.url),
  driveView: new URL('../src/views/app/DriveSectionView.vue', import.meta.url),
  mailCss: new URL('../src/views/app/mail-surface-view.css', import.meta.url),
  passCss: new URL('../src/views/app/pass-section-view.css', import.meta.url),
  passDetail: new URL('../src/views/app/pass/PassItemDetail.vue', import.meta.url),
  sheetsCss: new URL('../src/views/app/sheets-editor-view.css', import.meta.url),
  topBar: new URL('../src/layouts/modules/ShellTopBar.vue', import.meta.url)
}

test('round6 mail and shell surfaces protect mobile readability and edge targets', async () => {
  const [mailCss, topBar] = await Promise.all([readFile(files.mailCss, 'utf8'), readFile(files.topBar, 'utf8')])

  assert.match(mailCss, /\.mail-message-list__row-head strong\s*\{[\s\S]*text-overflow:\s*ellipsis/)
  assert.match(mailCss, /\.mail-message-list__row-head span\s*\{[\s\S]*flex:\s*0 0 auto/)
  assert.match(mailCss, /@media \(max-width: 1100px\)[\s\S]*border-bottom:\s*1px solid var\(--mm-border-strong\)/)
  assert.match(topBar, /@media \(max-width: 820px\)[\s\S]*\.top-bar__right\s*\{[\s\S]*padding-right:\s*4px/)
  assert.match(topBar, /@media \(max-width: 820px\)[\s\S]*\.avatar-chip\s*\{[\s\S]*min-width:\s*34px/)
})

test('round6 workspace cards and tables expose touch-safe affordances', async () => {
  const [calendarCss, dataTable, driveCss, driveView, passCss, passDetail] = await Promise.all([
    readFile(files.calendarCss, 'utf8'),
    readFile(files.dataTable, 'utf8'),
    readFile(files.driveCss, 'utf8'),
    readFile(files.driveView, 'utf8'),
    readFile(files.passCss, 'utf8'),
    readFile(files.passDetail, 'utf8')
  ])

  assert.match(calendarCss, /\.calendar-board__event\s*\{[\s\S]*line-height:\s*1\.35/)
  assert.match(calendarCss, /@media \(max-width: 820px\)[\s\S]*\.calendar-board__event\s*\{[\s\S]*padding:\s*10px/)
  assert.match(driveView, /drive-surface__card-menu/)
  assert.match(driveCss, /\.drive-surface__card-menu\s*\{[\s\S]*min-width:\s*44px/)
  assert.match(dataTable, /\.data-table td button\s*\{[\s\S]*min-width:\s*40px/)
  assert.match(dataTable, /@media \(max-width: 900px\)[\s\S]*\.data-table td button\s*\{[\s\S]*min-height:\s*40px/)
  assert.match(passDetail, /secondaryCard\.facts\.length/)
  assert.match(passCss, /\.pass-item-detail__card dl\s*\{[\s\S]*gap:\s*8px/)
})

test('round6 secondary workspace spacing leaves overflow and bottom-nav breathing room', async () => {
  const [adminView, collaborationView, commandCenterView, docsWorkspace, sheetsCss] = await Promise.all([
    readFile(files.adminView, 'utf8'),
    readFile(files.collaborationView, 'utf8'),
    readFile(files.commandCenterView, 'utf8'),
    readFile(files.docsWorkspace, 'utf8'),
    readFile(files.sheetsCss, 'utf8')
  ])

  assert.match(docsWorkspace, /@media \(max-width: 820px\)[\s\S]*\.docs-shell__list\s*\{[\s\S]*padding-bottom:\s*96px/)
  assert.match(sheetsCss, /@media \(max-width: 980px\)[\s\S]*\.sheets-editor__actions::after\s*\{[\s\S]*linear-gradient/)
  assert.match(collaborationView, /@media \(max-width: 820px\)[\s\S]*\.collaboration-activity\s*\{[\s\S]*margin-bottom:\s*96px/)
  assert.match(commandCenterView, /class="page-shell surface-grid command-center-page"/)
  assert.match(commandCenterView, /@media \(max-width: 820px\)[\s\S]*\.command-center-page\s*\{[\s\S]*gap:\s*20px/)
  assert.match(adminView, /\.admin-row strong,\s*\n\.admin-row span\s*\{[\s\S]*overflow-wrap:\s*anywhere/)
})
