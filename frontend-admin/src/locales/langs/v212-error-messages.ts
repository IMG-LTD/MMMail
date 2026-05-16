type ErrorMessageMap = App.I18n.Schema['errors'];

export const zhCNErrorMessages = {
  '30052': {
    title: '需要升级权益',
    message: '当前操作需要组织权益。原因：当前套餐未包含此能力。处理：升级套餐或联系管理员开通。'
  },
  '30053': {
    title: '权限不足',
    message: '当前账号无法执行此操作。原因：你没有目标资源的访问权限。处理：联系资源所有者或管理员授权。'
  },
  '30055': {
    title: '日历订阅不存在',
    message: '无法找到指定日历订阅。原因：订阅已删除或当前账号不可见。处理：刷新列表后重新选择订阅。'
  },
  '30056': {
    title: '外部邮箱账户不存在',
    message: '无法找到指定外部邮箱账户。原因：账户已移除或未完成绑定。处理：重新选择账户或添加新的外部邮箱。'
  },
  '40021': {
    title: '社区标题必填',
    message: '社区内容缺少标题。原因：标题为空会导致内容无法发布。处理：填写标题后重新提交。'
  },
  '40022': {
    title: '社区话题不存在',
    message: '无法找到指定社区话题。原因：话题已删除或当前空间不可见。处理：刷新话题列表后重新选择。'
  },
  '40023': {
    title: '社区帖子不存在',
    message: '无法找到指定社区帖子。原因：帖子已删除或链接已失效。处理：返回列表后重新打开帖子。'
  },
  '40024': {
    title: '社区评论不存在',
    message: '无法找到指定社区评论。原因：评论已删除或所属帖子不可见。处理：刷新帖子后重试。'
  },
  '40025': {
    title: '举报记录不存在',
    message: '无法找到指定举报记录。原因：举报已处理、删除或当前账号不可见。处理：刷新举报列表后重试。'
  },
  '40026': {
    title: '搜索关键词过短',
    message: '搜索关键词长度不足。原因：过短关键词会产生无效结果。处理：输入更完整的关键词后重新搜索。'
  },
  '40027': {
    title: '重建任务不存在',
    message: '无法找到搜索索引重建任务。原因：任务已完成、清理或标识无效。处理：刷新任务列表后重新查看。'
  },
  '40028': {
    title: '不支持的搜索模块',
    message: '当前搜索模块暂不支持。原因：请求的模块未接入全局搜索。处理：切换到支持的模块后重试。'
  },
  '40029': {
    title: '外部邮箱配置无效',
    message: '外部邮箱配置未通过校验。原因：服务器、端口或安全设置不完整。处理：检查配置后重新保存。'
  },
  '40121': {
    title: '外部邮箱授权失效',
    message: '外部邮箱授权不可用。原因：凭据过期、撤销或认证失败。处理：重新授权该外部邮箱账户。'
  },
  '40321': {
    title: '只能由作者操作',
    message: '当前操作仅作者可执行。原因：账号不是该社区内容作者。处理：切换作者账号或联系作者处理。'
  },
  '40322': {
    title: '需要社区管理员',
    message: '当前操作需要社区管理员权限。原因：普通成员不能管理该资源。处理：联系社区管理员执行操作。'
  },
  '40921': {
    title: '帖子已锁定',
    message: '帖子当前不可继续编辑或回复。原因：帖子已被锁定以避免新的变更。处理：解锁后再操作。'
  },
  '40922': {
    title: '话题仍有内容',
    message: '该话题暂不能删除。原因：话题下仍存在帖子或评论。处理：迁移或清理内容后重试。'
  },
  '42221': {
    title: '表格存在循环引用',
    message: '表格公式形成循环引用。原因：单元格互相依赖导致无法计算。处理：调整公式引用关系后重试。'
  },
  '50421': {
    title: '外部邮箱请求超时',
    message: '外部邮箱服务响应超时。原因：远端服务器未在限定时间内返回。处理：稍后重试或检查外部服务状态。'
  },
  '50521': {
    title: '外部邮箱请求受限',
    message: '外部邮箱服务拒绝了过多请求。原因：远端服务器触发频率限制。处理：等待限制解除后再同步。'
  }
} satisfies ErrorMessageMap;

export const enUSErrorMessages = {
  '30052': {
    title: 'Entitlement required',
    message:
      'What happened: this action needs an organization entitlement. Reason: the current plan does not include this capability. Next step: upgrade the plan or ask an administrator to enable it.'
  },
  '30053': {
    title: 'Permission denied',
    message:
      'What happened: this account cannot perform the action. Reason: you do not have access to the target resource. Next step: ask the owner or an administrator for permission.'
  },
  '30055': {
    title: 'Calendar subscription not found',
    message:
      'What happened: the calendar subscription could not be found. Reason: it was deleted or is not visible to this account. Next step: refresh the list and choose another subscription.'
  },
  '30056': {
    title: 'External mailbox account not found',
    message:
      'What happened: the external mailbox account could not be found. Reason: it was removed or has not been connected. Next step: choose another account or add a new external mailbox.'
  },
  '40021': {
    title: 'Community title required',
    message:
      'What happened: the community content has no title. Reason: content without a title cannot be published. Next step: enter a title and submit again.'
  },
  '40022': {
    title: 'Community topic not found',
    message:
      'What happened: the community topic could not be found. Reason: it was deleted or is not visible in this workspace. Next step: refresh the topic list and choose another topic.'
  },
  '40023': {
    title: 'Community post not found',
    message:
      'What happened: the community post could not be found. Reason: it was deleted or the link is no longer valid. Next step: return to the list and open the post again.'
  },
  '40024': {
    title: 'Community comment not found',
    message:
      'What happened: the community comment could not be found. Reason: it was deleted or the parent post is not visible. Next step: refresh the post and try again.'
  },
  '40025': {
    title: 'Community report not found',
    message:
      'What happened: the report could not be found. Reason: it was handled, deleted, or is not visible to this account. Next step: refresh the report list and try again.'
  },
  '40026': {
    title: 'Search query too short',
    message:
      'What happened: the search query is too short. Reason: short queries produce invalid results. Next step: enter a more complete query and search again.'
  },
  '40027': {
    title: 'Reindex job not found',
    message:
      'What happened: the search reindex job could not be found. Reason: it finished, was cleaned up, or the identifier is invalid. Next step: refresh the job list and check again.'
  },
  '40028': {
    title: 'Search module unsupported',
    message:
      'What happened: the requested search module is not supported. Reason: the module has not been connected to global search. Next step: switch to a supported module and try again.'
  },
  '40029': {
    title: 'External mailbox config invalid',
    message:
      'What happened: the external mailbox configuration is invalid. Reason: server, port, or security settings are incomplete. Next step: check the configuration and save again.'
  },
  '40121': {
    title: 'External mailbox authorization invalid',
    message:
      'What happened: the external mailbox authorization is unavailable. Reason: the credential expired, was revoked, or failed authentication. Next step: authorize the account again.'
  },
  '40321': {
    title: 'Author access required',
    message:
      'What happened: only the author can perform this action. Reason: this account is not the author of the community content. Next step: switch to the author account or ask the author to handle it.'
  },
  '40322': {
    title: 'Community admin required',
    message:
      'What happened: this action requires community administrator access. Reason: regular members cannot manage this resource. Next step: ask a community administrator to perform the action.'
  },
  '40921': {
    title: 'Post locked',
    message:
      'What happened: the post cannot be edited or replied to right now. Reason: it is locked to prevent new changes. Next step: unlock the post before continuing.'
  },
  '40922': {
    title: 'Topic not empty',
    message:
      'What happened: the topic cannot be deleted yet. Reason: posts or comments still exist under the topic. Next step: move or remove the content and try again.'
  },
  '42221': {
    title: 'Circular sheet reference',
    message:
      'What happened: the sheet formula contains a circular reference. Reason: cells depend on each other and cannot be calculated. Next step: adjust the formula references and try again.'
  },
  '50421': {
    title: 'External mailbox timeout',
    message:
      'What happened: the external mailbox service timed out. Reason: the remote server did not respond within the allowed time. Next step: try again later or check the external service status.'
  },
  '50521': {
    title: 'External mailbox rate limited',
    message:
      'What happened: the external mailbox service rejected too many requests. Reason: the remote server applied a rate limit. Next step: wait for the limit to clear before syncing again.'
  }
} satisfies ErrorMessageMap;
