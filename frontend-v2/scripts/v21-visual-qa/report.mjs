export const REPORT_PATH_SUFFIX = 'docs/superpowers/progress/v21-browser-visual-qa-report.md'

export async function writeVisualQaReport({ generatedAt, reportPath, results, writeFile }) {
  const groups = groupResults(results)
  const report = [
    '# v2.1 Browser Visual QA Report',
    '',
    `Generated at: ${generatedAt}`,
    '',
    `Total screenshots: ${results.length}`,
    '',
    '## UI group coverage',
    '',
    '| UI group | Scenario count | Screenshot count |',
    '| --- | ---: | ---: |',
    ...formatGroupRows(groups),
    '',
    '## Scenario evidence',
    '',
    '| UI group | Scenario | Viewport | Route | Required visible selectors | Screenshot evidence |',
    '| --- | --- | --- | --- | --- | --- |',
    ...results.map(formatEvidenceRow),
    '',
    '## Covered overlay and panel evidence',
    '',
    ...formatOverlayList(results),
    '',
    'Screenshots are evidence artifacts under `.tmp/` and are intentionally not committed.'
  ].join('\n')

  await writeFile(reportPath, `${report}\n`)
}

function groupResults(results) {
  const groups = new Map()
  for (const result of results) {
    groups.set(result.uiGroup, [...(groups.get(result.uiGroup) || []), result])
  }
  return groups
}

function formatGroupRows(groups) {
  return Array.from(groups.entries()).map(([group, items]) => {
    const scenarioCount = new Set(items.map(item => item.id)).size
    return `| ${group} | ${scenarioCount} | ${items.length} |`
  })
}

function formatEvidenceRow(item) {
  return `| ${item.uiGroup} | ${item.id} | ${item.viewport} | \`${item.route}\` | \`${item.checks.join('`, `')}\` | \`${item.screenshot}\` |`
}

function formatOverlayList(results) {
  const overlays = results.filter(item => item.kind === 'overlay')
  if (!overlays.length) {
    return ['- No overlay or panel evidence captured.']
  }
  return overlays.map(item => `- ${item.uiGroup}: \`${item.id}\` on \`${item.route}\``)
}
