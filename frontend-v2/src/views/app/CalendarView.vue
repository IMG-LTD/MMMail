<script setup lang="ts">
import { onMounted } from 'vue'
import { lt, useLocaleText } from '@/locales'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'

const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

onMounted(() => {
  void copilotPanel.loadCapabilities()
})

const miniCalendarDays = ['29', '30', '31', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17']

const calendars = [
  { key: 'work', name: lt('工作', '工作', 'Work'), active: true },
  { key: 'personal', name: lt('个人', '個人', 'Personal'), active: true },
  { key: 'tasks', name: lt('任务', '任務', 'Tasks'), active: false }
]

const viewModes = [
  { key: 'month', label: lt('月', '月', 'Month') },
  { key: 'week', label: lt('周', '週', 'Week') },
  { key: 'day', label: lt('日', '日', 'Day') },
  { key: 'agenda', label: lt('议程', '議程', 'Agenda') }
]
const dayHeaders = [
  { key: 'mon-13', label: lt('周一 13', '週一 13', 'Mon 13') },
  { key: 'tue-14', label: lt('周二 14', '週二 14', 'Tue 14') },
  { key: 'wed-15', label: lt('周三 15', '週三 15', 'Wed 15') },
  { key: 'thu-16', label: lt('周四 16', '週四 16', 'Thu 16') },
  { key: 'fri-17', label: lt('周五 17', '週五 17', 'Fri 17') }
]
const timeSlots = ['09 AM', '10 AM', '11 AM', '12 PM', '1 PM', '2 PM']

const events = [
  { id: 'design-review', title: lt('设计评审', '設計評審', 'Design Review'), meta: '09:00 – 10:30', style: { gridColumn: '2', gridRow: '3 / span 2' } },
  { id: 'security-review', title: lt('安全评审', '安全評審', 'Security Review'), meta: '11:00 – 12:00', style: { gridColumn: '4', gridRow: '5 / span 1' } }
]
</script>

<template>
  <section class="calendar-page">
    <div class="calendar-page__layout">
      <aside class="calendar-sidebar">
        <button class="calendar-sidebar__primary" type="button">{{ tr(lt('+ 新建事件', '+ 新增事件', '+ New Event')) }}</button>
        <button class="calendar-sidebar__secondary" type="button">{{ tr(lt('查找时间', '尋找時間', 'Find a time')) }}</button>
        <button class="calendar-sidebar__secondary" type="button" @click="copilotPanel.toggle()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>

        <article class="calendar-sidebar__panel">
          <header>
            <strong>{{ tr(lt('2023 年 11 月', '2023 年 11 月', 'November 2023')) }}</strong>
            <span>‹ ›</span>
          </header>
          <div class="calendar-sidebar__grid">
            <span v-for="day in miniCalendarDays" :key="day" :class="{ 'calendar-sidebar__day--active': day === '13' || day === '14' }">
              {{ day }}
            </span>
          </div>
        </article>

        <article class="calendar-sidebar__panel">
          <span class="section-label">{{ tr(lt('我的日历', '我的日曆', 'My Calendars')) }}</span>
          <label v-for="calendar in calendars" :key="calendar.key" class="calendar-sidebar__toggle">
            <span class="calendar-sidebar__check" :class="{ 'calendar-sidebar__check--active': calendar.active }" />
            <span>{{ tr(calendar.name) }}</span>
          </label>
        </article>
      </aside>

      <div class="calendar-board">
        <header class="calendar-board__toolbar">
          <div>
            <span class="section-label">{{ tr(lt('十一月', '十一月', 'November')) }}</span>
            <h1>13 – 19</h1>
          </div>
          <div class="calendar-board__actions">
            <button type="button">{{ tr(lt('今天', '今天', 'Today')) }}</button>
            <div class="calendar-board__switcher">
              <button
                v-for="mode in viewModes"
                :key="mode.key"
                type="button"
                :class="{ 'calendar-board__switcher--active': mode.key === 'week' }"
              >
                {{ tr(mode.label) }}
              </button>
            </div>
          </div>
        </header>

        <div class="calendar-board__schedule">
          <div class="calendar-board__head">
            <span />
            <strong v-for="day in dayHeaders" :key="day.key">{{ tr(day.label) }}</strong>
          </div>

          <div v-for="slot in timeSlots" :key="slot" class="calendar-board__row">
            <span class="calendar-board__time">{{ slot }}</span>
            <span v-for="day in dayHeaders" :key="`${day.key}-${slot}`" class="calendar-board__cell" />
          </div>

          <article
            v-for="event in events"
            :key="event.id"
            class="calendar-board__event"
            :style="event.style"
          >
            <strong>{{ tr(event.title) }}</strong>
            <span>{{ event.meta }}</span>
          </article>
        </div>
      </div>

      <aside class="calendar-drawer">
        <div class="calendar-drawer__head">
          <span class="section-label">{{ tr(lt('编辑事件', '編輯事件', 'Edit Event')) }}</span>
          <span>×</span>
        </div>
        <strong>{{ tr(lt('设计评审', '設計評審', 'Design Review')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('周一，11 月 13 日 · 上午 9:00 – 10:30', '週一，11 月 13 日 · 上午 9:00 – 10:30', 'Monday, Nov 13 · 9:00 AM – 10:30 AM')) }}</p>

        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('参与人', '參與者', 'Guests')) }}</span>
          <p>{{ tr(lt('3 位参与人', '3 位參與者', '3 guests')) }}</p>
        </div>
        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('地点', '地點', 'Location')) }}</span>
          <p>{{ tr(lt('A 会议室', 'A 會議室', 'Meeting Room A')) }}</p>
        </div>
        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('备注', '備註', 'Notes')) }}</span>
          <p>{{ tr(lt('评审日历模块的 Q4 UI 更新，重点检查对比度与间距。', '評審日曆模組的 Q4 UI 更新，重點檢查對比度與間距。', 'Review Q4 UI updates for the calendar module. Focus on contrast and spacing.')) }}</p>
        </div>

        <div class="calendar-drawer__footer">
          <button type="button">{{ tr(lt('取消', '取消', 'Cancel')) }}</button>
          <button class="calendar-drawer__save" type="button">{{ tr(lt('保存', '儲存', 'Save')) }}</button>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.calendar-page {
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.calendar-page__layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 260px;
  min-height: calc(100vh - 56px);
}

.calendar-sidebar,
.calendar-drawer {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 18px;
  background: var(--mm-side-surface);
}

.calendar-sidebar {
  border-right: 1px solid var(--mm-border);
}

.calendar-drawer {
  border-left: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.calendar-sidebar__primary,
.calendar-sidebar__secondary,
.calendar-board__toolbar button,
.calendar-drawer__footer button {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 10px;
}

.calendar-sidebar__primary,
.calendar-drawer__save {
  border: 0;
  background: linear-gradient(180deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
  color: #fff;
  font-weight: 600;
}

.calendar-sidebar__secondary,
.calendar-board__toolbar button,
.calendar-drawer__footer button:not(.calendar-drawer__save) {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.calendar-sidebar__panel {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius);
  background: var(--mm-card);
}

.calendar-sidebar__panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.calendar-sidebar__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.calendar-sidebar__grid span,
.calendar-sidebar__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.calendar-sidebar__grid span {
  width: 100%;
  min-height: 28px;
  border-radius: 8px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.calendar-sidebar__day--active {
  background: var(--mm-accent-soft);
  color: var(--mm-primary) !important;
  font-weight: 600;
}

.calendar-sidebar__toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--mm-text);
}

.calendar-sidebar__check {
  width: 14px;
  height: 14px;
  border: 1px solid var(--mm-border-strong);
  border-radius: 4px;
  background: var(--mm-card);
}

.calendar-sidebar__check--active {
  border-color: var(--mm-primary);
  background: var(--mm-primary);
  box-shadow: inset 0 0 0 2px var(--mm-card);
}

.calendar-board {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
}

.calendar-board__toolbar {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 24px 14px;
  border-bottom: 1px solid var(--mm-border);
}

.calendar-board__toolbar h1 {
  margin: 6px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.calendar-board__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calendar-board__switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.calendar-board__switcher button {
  min-width: 64px;
}

.calendar-board__switcher--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.calendar-board__schedule {
  position: relative;
  display: grid;
  grid-template-columns: 72px repeat(5, minmax(0, 1fr));
  grid-auto-rows: minmax(72px, auto);
  overflow: hidden;
}

.calendar-board__head,
.calendar-board__row {
  display: contents;
}

.calendar-board__head strong,
.calendar-board__time,
.calendar-board__cell {
  border-right: 1px solid var(--mm-border);
  border-bottom: 1px solid var(--mm-border);
}

.calendar-board__head strong {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  font-size: 12px;
}

.calendar-board__time {
  display: flex;
  align-items: start;
  justify-content: center;
  padding-top: 12px;
  color: var(--mm-text-secondary);
  font-size: 11px;
}

.calendar-board__cell {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
}

.calendar-board__event {
  z-index: 1;
  margin: 12px 10px;
  padding: 12px;
  border-radius: 14px;
  background: var(--mm-accent-soft);
  border: 1px solid var(--mm-accent-border);
  align-self: stretch;
}

.calendar-board__event strong {
  display: block;
  color: var(--mm-primary);
}

.calendar-board__event span {
  display: block;
  margin-top: 6px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.calendar-drawer__head,
.calendar-drawer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.calendar-drawer strong {
  font-size: 22px;
  line-height: 1.15;
}

.calendar-drawer__block {
  display: grid;
  gap: 6px;
  padding: 14px 0;
  border-top: 1px solid var(--mm-border);
}

.calendar-drawer__block p {
  margin: 0;
  color: var(--mm-text);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1120px) {
  .calendar-page__layout {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .calendar-drawer {
    display: none;
  }
}

@media (max-width: 820px) {
  .calendar-page__layout {
    grid-template-columns: 1fr;
  }

  .calendar-sidebar {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }

  .calendar-board__toolbar {
    flex-direction: column;
    align-items: start;
  }
}
</style>
