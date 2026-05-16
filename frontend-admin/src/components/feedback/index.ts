// @/components/feedback/index.ts — 五态组件统一入口
//
// 用法：
//   import { EmptyState, ErrorState, LoadingState, PageStateWrapper } from '@/components/feedback';
//
// 设计原则（spec §22.2）：
//   - 不引入新 UI 库，全部基于 naive-ui 原生组件包装
//   - 文案默认走 page.state.* + errors.{code}.* 两套 i18n key
//   - 全部组件支持 compact 模式以适配嵌入式场景（侧边栏、列表内、抽屉内）

export { default as EmptyState } from './EmptyState.vue';
export { default as ErrorState } from './ErrorState.vue';
export { default as LoadingState } from './LoadingState.vue';
export { default as PageStateWrapper } from './PageStateWrapper.vue';
