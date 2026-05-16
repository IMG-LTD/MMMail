import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const spec = await readFile(new URL('../docs/v212-migration-spec.md', import.meta.url), 'utf8');

function extractRetentionTable(source) {
  const start = source.indexOf('### 21.5 删除/保留策略统一表');
  const end = source.indexOf('\n---', start);
  assert.notEqual(start, -1, 'missing §21.5 retention policy section');
  assert.notEqual(end, -1, 'missing §21.5 section end marker');

  return source.slice(start, end);
}

test('v2.1.2 retention policy table covers every new module family', () => {
  const table = extractRetentionTable(spec);
  const requiredRows = [
    '钱包账户',
    'VPN 配置',
    '会议房间',
    '联系人',
    'SimpleLogin Alias',
    'Standard Notes 笔记',
    'TOTP 密钥',
    '邮件规则',
    '邮件外部账户',
    'Drive 版本',
    'Drive E2EE 分享链接',
    '自定义域名',
    'Web Push 订阅',
    '社区帖子',
    '全局搜索索引',
    '命令面板偏好',
    '日历订阅',
    'CRDT 协同快照',
    '表格公式',
    '看板任务',
    '通知实时事件',
    '登录安全事件',
    'Admin 计费记录'
  ];

  for (const row of requiredRows) {
    assert.match(table, new RegExp(`\\|\\s*${row.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\|`));
  }
});
