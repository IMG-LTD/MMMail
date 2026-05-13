# Backend v2.1 Calendar Runtime Bridge 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [x]`）语法来跟踪进度。

**目标：** 让 v2.1 Calendar Community runtime 从前端保存动作到后端 `/api/v2/calendar/*` 合同都走真实服务、真实持久化和真实 entitlement gate，不再保留保存占位提示。

**架构：** `mmmail-server` 新增 v2 Calendar controller 和 settings 服务薄桥接，复用 v1 `CalendarService`、`CalendarAvailabilityService`、`UserPreferenceService`、审计和 access gate；`frontend-v2` 补齐 Calendar API mutation、抽屉草稿状态和保存后重载。Premium resources/bookings 继续由 v2 access gate 拦截，不增加假资源数据。

**技术栈：** Java 21 records, Spring Boot MVC, Bean Validation, MyBatis-Plus, JUnit 5, MockMvc, Vue 3 `<script setup>`, TypeScript, node:test, Maven, pnpm。

---

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CalendarRuntimeBridgeTest.java`
  覆盖 v2 Calendar create/list/agenda/update/delete/availability/settings，以及 premium resources/bookings gate。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21CalendarController.java`
  暴露 `/api/v2/calendar/events`、`/api/v2/calendar/events/{eventId}`、`/api/v2/calendar/availability`、`/api/v2/calendar/settings`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/CalendarSettingsService.java`
  将 Calendar settings 映射到真实 `user_preference.timezone`，并显式拒绝未持久化的 week/working-hour 变更。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/UpdateCalendarSettingsRequest.java`
  定义 v2 Calendar settings PATCH 输入。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/CalendarSettingsVo.java`
  定义 v2 Calendar settings 输出。
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/service/UserPreferenceService.java`
  增加只更新 Calendar timezone 的公开方法，复用现有 preference 行和审计。
- 修改：`frontend-v2/src/service/api/calendar.ts`
  增加 create/update/delete event API，settings update 改为 PATCH。
- 修改：`frontend-v2/src/views/app/calendar/calendar-types.ts`
  增加抽屉草稿类型。
- 修改：`frontend-v2/src/views/app/calendar/CalendarEventDrawer.vue`
  把只读 `:value` 表单改为本地草稿状态，并在 save 时 emit 草稿。
- 修改：`frontend-v2/src/views/app/CalendarView.vue`
  将 `saveEventDraft()` 改为真实 create/update/delete 后重载，不再显示占位文案。
- 修改：`frontend-v2/tests/calendar-workspace-contract.test.mjs`
  增加 Calendar mutation、PATCH settings、抽屉草稿和占位文案删除的静态契约断言。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  记录本切片开始、完成状态、验证命令和提交号。

## 任务 1：记录活动切片并编写后端失败测试

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CalendarRuntimeBridgeTest.java`

- [x] **步骤 1：记录活动切片**

将 `## Active Backend Slice` 更新为：

```markdown
## Active Backend Slice

- Slice: `backend-v21-calendar-runtime-bridge`
- Status: `in_progress`
- Started: `2026-05-13`
- Scope: v2 Calendar runtime bridge, event mutations, availability, settings timezone persistence, frontend save wiring
- Verification target: `BackendV21CalendarRuntimeBridgeTest`, v1 Calendar regression, v2 gate regression, frontend v2 contract tests
```

- [x] **步骤 2：新增 v2 Calendar runtime bridge 失败测试**

创建 `BackendV21CalendarRuntimeBridgeTest.java`，测试命名和 helper 风格沿用 `CalendarReleaseBlockingIntegrationTest`：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21CalendarRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21EventsShouldUseRuntimeCalendarCrudAndAvailability() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v21-calendar-owner-" + suffix + "@mmmail.local";
        String token = register(ownerEmail, "V21 Calendar Owner");

        String eventId = createV21Event(token, """
                {
                  "title": "V2 runtime review",
                  "location": "Room V2",
                  "startAt": "2026-05-22T09:00:00",
                  "endAt": "2026-05-22T10:00:00",
                  "timezone": "Asia/Shanghai",
                  "reminderMinutes": 15,
                  "attendees": []
                }
                """);

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-05-22T00:00:00")
                        .param("to", "2026-05-23T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(eventId))
                .andExpect(jsonPath("$.data[0].title").value("V2 runtime review"));

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("view", "agenda")
                        .param("days", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("V2 runtime review"));

        mockMvc.perform(post("/api/v2/calendar/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-05-22T09:30:00",
                                  "endAt": "2026-05-22T09:45:00",
                                  "attendeeEmails": ["%s"]
                                }
                                """.formatted(ownerEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.busyCount").value(1));

        mockMvc.perform(patch("/api/v2/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "V2 runtime review updated",
                                  "location": "Room V2",
                                  "startAt": "2026-05-22T09:00:00",
                                  "endAt": "2026-05-22T10:30:00",
                                  "timezone": "Asia/Shanghai",
                                  "reminderMinutes": 30,
                                  "attendees": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("V2 runtime review updated"))
                .andExpect(jsonPath("$.data.reminderMinutes").value(30));

        mockMvc.perform(delete("/api/v2/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
```

在同一测试类继续加入：

```java
@Test
void v21CalendarSettingsShouldPersistTimezoneThroughUserPreference() throws Exception {
    String token = register("v21-calendar-settings-" + System.nanoTime() + "@mmmail.local", "V21 Calendar Settings");

    mockMvc.perform(get("/api/v2/calendar/settings")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.defaultTimezone").value("UTC"))
            .andExpect(jsonPath("$.data.weekStartsOn").value("monday"))
            .andExpect(jsonPath("$.data.workingHours[0]").value("09:00"));

    mockMvc.perform(patch("/api/v2/calendar/settings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "defaultTimezone": "Asia/Shanghai",
                              "weekStartsOn": "monday",
                              "workingHours": ["09:00", "18:00"]
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.defaultTimezone").value("Asia/Shanghai"));

    mockMvc.perform(get("/api/v2/calendar/settings")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.defaultTimezone").value("Asia/Shanghai"));
}

@Test
void premiumCalendarResourcesShouldRemainEntitlementGated() throws Exception {
    String token = register("v21-calendar-premium-" + System.nanoTime() + "@mmmail.local", "V21 Calendar Premium");

    mockMvc.perform(get("/api/v2/calendar/resources")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));

    mockMvc.perform(post("/api/v2/calendar/bookings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "eventId": "1",
                              "resourceId": "room-a",
                              "startAt": "2026-05-22T09:00:00",
                              "endAt": "2026-05-22T10:00:00"
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
}
```

Helper 方法使用真实注册和真实 v2 endpoint：

```java
private String createV21Event(String token, String payload) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v2/calendar/events")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();
    return readJson(result).at("/data/id").asText();
}

private String register(String email, String displayName) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "email": "%s",
                              "password": "%s",
                              "displayName": "%s"
                            }
                            """.formatted(email, PASSWORD, displayName)))
            .andExpect(status().isOk())
            .andReturn();
    return readJson(result).at("/data/accessToken").asText();
}

private JsonNode readJson(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
}
```

- [x] **步骤 3：运行后端红灯测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CalendarRuntimeBridgeTest
```

期望结果：测试失败，错误表现为 `/api/v2/calendar/events` 或 `/api/v2/calendar/settings` 没有真实 controller/runtime handler。若失败原因是编译错误，先确认测试 import 是否遗漏 `patch`、`delete`、`ErrorCode`。

## 任务 2：实现后端 v2 Calendar controller 和 settings 服务

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21CalendarController.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/CalendarSettingsService.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/UpdateCalendarSettingsRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/CalendarSettingsVo.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/service/UserPreferenceService.java`

- [x] **步骤 1：增加 settings DTO 和 VO**

`UpdateCalendarSettingsRequest.java`：

```java
package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCalendarSettingsRequest(
        @NotBlank @Size(max = 64) String defaultTimezone,
        @NotBlank @Size(max = 32) String weekStartsOn,
        @NotEmpty List<@NotBlank @Size(max = 8) String> workingHours
) {
}
```

`CalendarSettingsVo.java`：

```java
package com.mmmail.server.model.vo;

import java.util.List;

public record CalendarSettingsVo(
        String defaultTimezone,
        String weekStartsOn,
        List<String> workingHours
) {
}
```

- [x] **步骤 2：在 UserPreferenceService 中增加真实 timezone 更新方法**

新增公开方法，复用 `findOrCreatePreference()` 的完整默认值，避免只写一个不完整 preference 行：

```java
@Transactional
public UserPreferenceVo updateCalendarTimezone(Long userId, String timezone, String ipAddress) {
    UserAccount user = requireUser(userId);
    LocalDateTime now = LocalDateTime.now();
    UserPreference preference = findOrCreatePreference(userId);
    preference.setTimezone(CalendarTimezoneResolver.normalizeOrDefault(timezone, "UTC", "Calendar timezone"));
    applyAuthenticatorDefaults(preference);
    preference.setUpdatedAt(now);

    if (preference.getId() == null) {
        preference.setCreatedAt(now);
        preference.setDeleted(0);
        userPreferenceMapper.insert(preference);
    } else {
        userPreferenceMapper.updateById(preference);
    }

    auditService.record(userId, "CALENDAR_SETTINGS_UPDATED", "Timezone updated", ipAddress);
    return toVo(user, preference);
}
```

补充 import：

```java
import com.mmmail.server.util.CalendarTimezoneResolver;
```

- [x] **步骤 3：实现 CalendarSettingsService**

```java
package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.UpdateCalendarSettingsRequest;
import com.mmmail.server.model.vo.CalendarSettingsVo;
import com.mmmail.server.model.vo.UserPreferenceVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CalendarSettingsService {

    private static final String SUPPORTED_WEEK_START = "monday";
    private static final List<String> SUPPORTED_WORKING_HOURS = List.of("09:00", "18:00");

    private final UserPreferenceService userPreferenceService;

    public CalendarSettingsService(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    public CalendarSettingsVo getSettings(Long userId) {
        return toSettings(userPreferenceService.getProfile(userId));
    }

    public CalendarSettingsVo updateSettings(Long userId, UpdateCalendarSettingsRequest request, String ipAddress) {
        assertSupportedCalendarFields(request);
        return toSettings(userPreferenceService.updateCalendarTimezone(userId, request.defaultTimezone(), ipAddress));
    }

    private void assertSupportedCalendarFields(UpdateCalendarSettingsRequest request) {
        if (!SUPPORTED_WEEK_START.equals(request.weekStartsOn())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar week start is not configurable in Community runtime");
        }
        if (!SUPPORTED_WORKING_HOURS.equals(request.workingHours())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar working hours are not configurable in Community runtime");
        }
    }

    private CalendarSettingsVo toSettings(UserPreferenceVo preference) {
        return new CalendarSettingsVo(preference.timezone(), SUPPORTED_WEEK_START, SUPPORTED_WORKING_HOURS);
    }
}
```

这个服务只持久化已有真实字段 `user_preference.timezone`。`weekStartsOn` 和 `workingHours` 没有现有存储字段，因此不同值必须显式 400，不允许表面返回成功。

- [x] **步骤 4：实现 V21CalendarController**

```java
@RestController
@Validated
@RequestMapping("/api/v2/calendar")
public class V21CalendarController {

    private final CalendarService calendarService;
    private final CalendarAvailabilityService calendarAvailabilityService;
    private final CalendarSettingsService calendarSettingsService;

    public V21CalendarController(
            CalendarService calendarService,
            CalendarAvailabilityService calendarAvailabilityService,
            CalendarSettingsService calendarSettingsService
    ) {
        this.calendarService = calendarService;
        this.calendarAvailabilityService = calendarAvailabilityService;
        this.calendarSettingsService = calendarSettingsService;
    }

    @GetMapping("/events")
    public Result<?> events(
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer days,
            HttpServletRequest httpRequest
    ) {
        if (!StringUtils.hasText(view)) {
            return Result.success(calendarService.listEvents(SecurityUtils.currentUserId(), from, to));
        }
        if ("agenda".equalsIgnoreCase(view.trim())) {
            return Result.success(calendarService.listAgenda(SecurityUtils.currentUserId(), days, httpRequest.getRemoteAddr()));
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar view must be agenda");
    }
}
```

在同一 controller 增加 mutation、availability、settings 方法：

```java
@PostMapping("/events")
public Result<CalendarEventDetailVo> createEvent(
        @Valid @RequestBody CreateCalendarEventRequest request,
        HttpServletRequest httpRequest
) {
    return Result.success(calendarService.createEvent(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
}

@PatchMapping("/events/{eventId}")
public Result<CalendarEventDetailVo> updateEvent(
        @PathVariable Long eventId,
        @Valid @RequestBody UpdateCalendarEventRequest request,
        HttpServletRequest httpRequest
) {
    return Result.success(calendarService.updateEvent(SecurityUtils.currentUserId(), eventId, request, httpRequest.getRemoteAddr()));
}

@DeleteMapping("/events/{eventId}")
public Result<Void> deleteEvent(@PathVariable Long eventId, HttpServletRequest httpRequest) {
    calendarService.deleteEvent(SecurityUtils.currentUserId(), eventId, httpRequest.getRemoteAddr());
    return Result.success(null);
}

@PostMapping("/availability")
public Result<CalendarAvailabilityVo> availability(
        @Valid @RequestBody QueryCalendarAvailabilityRequest request,
        HttpServletRequest httpRequest
) {
    return Result.success(calendarAvailabilityService.queryAvailability(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
}

@GetMapping("/settings")
public Result<CalendarSettingsVo> settings() {
    return Result.success(calendarSettingsService.getSettings(SecurityUtils.currentUserId()));
}

@PatchMapping("/settings")
public Result<CalendarSettingsVo> updateSettings(
        @Valid @RequestBody UpdateCalendarSettingsRequest request,
        HttpServletRequest httpRequest
) {
    return Result.success(calendarSettingsService.updateSettings(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
}
```

必要 import：

```java
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateCalendarEventRequest;
import com.mmmail.server.model.dto.QueryCalendarAvailabilityRequest;
import com.mmmail.server.model.dto.UpdateCalendarEventRequest;
import com.mmmail.server.model.dto.UpdateCalendarSettingsRequest;
import com.mmmail.server.model.vo.CalendarAvailabilityVo;
import com.mmmail.server.model.vo.CalendarEventDetailVo;
import com.mmmail.server.model.vo.CalendarSettingsVo;
import com.mmmail.server.service.CalendarAvailabilityService;
import com.mmmail.server.service.CalendarService;
import com.mmmail.server.service.CalendarSettingsService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
```

不要在 controller 中添加 `/resources` 或 `/bookings` 实现。Community runtime 必须继续通过 v2 access gate 返回 `V2_ENTITLEMENT_REQUIRED`，不能返回空数组或模拟 booking 成功。

- [x] **步骤 5：运行后端绿灯和回归**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CalendarRuntimeBridgeTest,CalendarReleaseBlockingIntegrationTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

若 `BackendV21CalendarRuntimeBridgeTest` 通过但 v1 Calendar 回归失败，只能修桥接层或共享服务的真实问题；不要降低断言或改 gate 规则绕过失败。

## 任务 3：编写前端契约失败测试

**文件：**
- 修改：`frontend-v2/tests/calendar-workspace-contract.test.mjs`

- [x] **步骤 1：扩展静态契约断言**

在既有测试中追加对 mutation API、PATCH settings、抽屉草稿和占位文案删除的断言：

```js
const drawerFile = new URL('../src/views/app/calendar/CalendarEventDrawer.vue', import.meta.url)

test('calendar workspace persists drawer drafts through v2 mutation APIs', async () => {
  const [api, view, drawer] = await Promise.all([
    readFile(apiFile, 'utf8'),
    readFile(viewFile, 'utf8'),
    readFile(drawerFile, 'utf8')
  ])

  assert.match(api, /export function createCalendarEvent/)
  assert.match(api, /export function updateCalendarEvent/)
  assert.match(api, /export function deleteCalendarEvent/)
  assert.match(api, /httpClient\.post<ApiResponse<CalendarEventMutationResult>>\('\/api\/v2\/calendar\/events'/)
  assert.match(api, /httpClient\.patch<ApiResponse<CalendarEventMutationResult>>\(`\/api\/v2\/calendar\/events\/\$\{eventId\}`/)
  assert.match(api, /httpClient\.delete<ApiResponse<null>>\(`\/api\/v2\/calendar\/events\/\$\{eventId\}`/)
  assert.match(api, /httpClient\.patch<ApiResponse<CalendarSettings>>\('\/api\/v2\/calendar\/settings'/)
  assert.match(view, /createCalendarEvent/)
  assert.match(view, /updateCalendarEvent/)
  assert.match(view, /buildCalendarEventPayload/)
  assert.match(view, /await loadCalendar\(\)/)
  assert.match(drawer, /CalendarEventDraft/)
  assert.match(drawer, /v-model="draft\.title"/)
  assert.match(drawer, /emit\('save', \{ \.\.\.draft \}\)/)
  assert.doesNotMatch(view, /Calendar save requires an API endpoint/)
})
```

- [x] **步骤 2：运行前端红灯测试**

```bash
pnpm --dir frontend-v2 test
```

期望结果：新增契约测试失败，指出缺少 `createCalendarEvent`、`updateCalendarEvent`、`deleteCalendarEvent` 或占位文案仍存在。

## 任务 4：实现前端 Calendar mutation API 和抽屉保存

**文件：**
- 修改：`frontend-v2/src/service/api/calendar.ts`
- 修改：`frontend-v2/src/views/app/calendar/calendar-types.ts`
- 修改：`frontend-v2/src/views/app/calendar/CalendarEventDrawer.vue`
- 修改：`frontend-v2/src/views/app/CalendarView.vue`

- [x] **步骤 1：补齐 Calendar API mutation**

在 `calendar.ts` 增加 payload 类型：

```ts
export interface CalendarAttendeeInput {
  displayName?: string
  email: string
}

export interface CalendarEventMutationPayload {
  allDay?: boolean
  attendees: CalendarAttendeeInput[]
  description?: string
  endAt: string
  location?: string
  reminderMinutes?: number | null
  startAt: string
  timezone?: string
  title: string
}

export interface CalendarEventMutationResult {
  allDay: boolean
  attendees: CalendarAttendeeInput[]
  canDelete: boolean
  canEdit: boolean
  description: string | null
  endAt: string
  id: string
  location: string | null
  ownerEmail: string | null
  reminderMinutes: number | null
  shared: boolean
  sharePermission: string
  startAt: string
  timezone: string
  title: string
  updatedAt: string
}
```

增加方法，并将 settings update 改为 PATCH：

```ts
export function createCalendarEvent(token: string, body: CalendarEventMutationPayload) {
  return httpClient.post<ApiResponse<CalendarEventMutationResult>>('/api/v2/calendar/events', { body, token })
}

export function updateCalendarEvent(token: string, eventId: string, body: CalendarEventMutationPayload) {
  return httpClient.patch<ApiResponse<CalendarEventMutationResult>>(`/api/v2/calendar/events/${eventId}`, { body, token })
}

export function deleteCalendarEvent(token: string, eventId: string) {
  return httpClient.delete<ApiResponse<null>>(`/api/v2/calendar/events/${eventId}`, { token })
}

export function updateCalendarSettings(token: string, body: CalendarSettings) {
  return httpClient.patch<ApiResponse<CalendarSettings>>('/api/v2/calendar/settings', { body, token })
}
```

- [x] **步骤 2：增加 CalendarEventDraft 类型**

在 `calendar-types.ts` 增加：

```ts
export interface CalendarEventDraft {
  allDay: boolean
  description: string
  endAt: string
  location: string
  reminderMinutes: number | null
  startAt: string
  timezone: string
  title: string
}
```

- [x] **步骤 3：把 CalendarEventDrawer 改为真实草稿表单**

在 drawer 中使用 `reactive`、`watch` 初始化草稿：

```ts
const props = defineProps<{
  availability: CalendarAvailability | null
  loading: boolean
  open: boolean
  saveError: string
  selectedItem: CalendarSurfaceItem | null
}>()

const emit = defineEmits<{
  close: []
  retry: [draft: CalendarEventDraft]
  save: [draft: CalendarEventDraft]
}>()

const draft = reactive<CalendarEventDraft>(createDraft(props.selectedItem))

watch(() => [props.open, props.selectedItem?.id], () => {
  Object.assign(draft, createDraft(props.selectedItem))
})
```

辅助函数保持短函数：

```ts
function createDraft(item: CalendarSurfaceItem | null): CalendarEventDraft {
  const startAt = item?.startAt || '2026-05-22T09:00:00'
  return {
    allDay: item?.allDay || false,
    description: '',
    endAt: item?.endAt || '2026-05-22T10:00:00',
    location: item?.location || 'Nexa Meet / Room A',
    reminderMinutes: 15,
    startAt,
    timezone: 'UTC',
    title: item?.title || 'Privacy and security review'
  }
}

function emitSave() {
  emit('save', { ...draft })
}
```

模板输入改为 `v-model`：

```vue
<input aria-label="Event title" v-model="draft.title">
<input aria-label="Start time" type="datetime-local" v-model="draft.startAt">
<input aria-label="End time" type="datetime-local" v-model="draft.endAt">
<input aria-label="Location" v-model="draft.location">
<textarea aria-label="Notes" v-model="draft.description" />
<button class="calendar-event-drawer__save" type="button" @click="emitSave">Save</button>
<button v-if="saveError" class="calendar-save-retry" type="button" @click="$emit('retry', { ...draft })">Retry</button>
```

- [x] **步骤 4：在 CalendarView 中接入真实保存**

更新 import：

```ts
import {
  createCalendarEvent,
  listCalendarAgenda,
  listCalendarEvents,
  queryCalendarAvailability,
  updateCalendarEvent,
  type CalendarEventMutationPayload
} from '@/service/api/calendar'
import type { CalendarEventDraft } from './calendar/calendar-types'
```

替换 `saveEventDraft()`：

```ts
async function saveEventDraft(draft: CalendarEventDraft) {
  const token = authStore.accessToken
  if (!token) {
    calendarSaveError.value = 'Sign in to save calendar events.'
    return
  }
  try {
    calendarSaveError.value = ''
    const payload = buildCalendarEventPayload(draft)
    const response = selectedItem.value?.id
      ? await updateCalendarEvent(token, selectedItem.value.id, payload)
      : await createCalendarEvent(token, payload)
    selectedEventId.value = response.data.id
    eventDrawerOpen.value = false
    await loadCalendar()
  } catch (error) {
    calendarSaveError.value = resolveErrorMessage(error)
  }
}

function buildCalendarEventPayload(draft: CalendarEventDraft): CalendarEventMutationPayload {
  return {
    allDay: draft.allDay,
    attendees: [],
    description: draft.description,
    endAt: draft.endAt,
    location: draft.location,
    reminderMinutes: draft.reminderMinutes,
    startAt: draft.startAt,
    timezone: draft.timezone,
    title: draft.title
  }
}
```

保留 `@retry="saveEventDraft"` 和 `@save="saveEventDraft"`，因为 drawer 现在会传入草稿。

- [x] **步骤 5：前端测试和类型检查**

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

如 `typecheck` 暴露 `datetime-local` 的秒格式问题，统一在 helper 中转换为 `YYYY-MM-DDTHH:mm:ss`；不要通过 `any` 或删除类型断言跳过。

## 任务 5：联合验证并提交实现

**文件：**
- 本计划任务 1-4 中列出的源码和测试文件

- [x] **步骤 1：运行后端目标验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CalendarRuntimeBridgeTest,CalendarReleaseBlockingIntegrationTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

- [x] **步骤 2：运行前端目标验证**

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

- [x] **步骤 3：检查只暂存本切片文件**

```bash
git status --short --branch
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CalendarRuntimeBridgeTest.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21CalendarController.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/service/CalendarSettingsService.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/UpdateCalendarSettingsRequest.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/CalendarSettingsVo.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/service/UserPreferenceService.java
git add frontend-v2/src/service/api/calendar.ts
git add frontend-v2/src/views/app/calendar/calendar-types.ts
git add frontend-v2/src/views/app/calendar/CalendarEventDrawer.vue
git add frontend-v2/src/views/app/CalendarView.vue
git add frontend-v2/tests/calendar-workspace-contract.test.mjs
git diff --cached --check
git diff --cached --stat
```

不要暂存 `.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、仓库根 `frontend/`。

- [x] **步骤 4：提交实现**

```bash
git commit -m "feat(backend-v21): add calendar runtime bridge"
git status --short --branch
```

## 任务 6：更新进度文档并提交

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-calendar-runtime-bridge.md`

- [x] **步骤 1：更新计划复选框和进度文档**

将已完成步骤改为 `[x]`。将 `v21-implementation-progress.md` 中本切片更新为：

```markdown
## Active Backend Slice

- Slice: `backend-v21-calendar-runtime-bridge`
- Status: `completed`
- Started: `2026-05-13`
- Completed: `2026-05-13`
- Scope: v2 Calendar runtime bridge, event mutations, availability, settings timezone persistence, frontend save wiring
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CalendarRuntimeBridgeTest,CalendarReleaseBlockingIntegrationTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `pnpm --dir frontend-v2 build`
```

在 Backend Slices 表中新增或更新 `backend-v21-calendar-runtime-bridge` 为 `completed`，并写入实现提交号。

- [x] **步骤 2：提交文档进度**

```bash
git status --short --branch
git add -f docs/superpowers/plans/2026-05-13-backend-v21-calendar-runtime-bridge.md
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update calendar runtime bridge progress"
git status --short --branch
```

## 验收标准

- `/api/v2/calendar/events` 支持 list 和 `view=agenda`，并复用 v1 Calendar query/agenda 服务。
- `/api/v2/calendar/events` POST、`/api/v2/calendar/events/{eventId}` PATCH/DELETE 使用真实 Calendar persistence、权限、审计、attendee sync 和 quota 逻辑。
- `/api/v2/calendar/availability` 使用真实 CalendarAvailabilityService。
- `/api/v2/calendar/settings` GET/PATCH 使用真实 `user_preference.timezone`；没有存储字段的 week/working-hour 变更显式报错。
- `/api/v2/calendar/resources` 和 `/api/v2/calendar/bookings` 对 Community 用户继续返回 `V2_ENTITLEMENT_REQUIRED`，没有假成功数据。
- `CalendarView.vue` 不再包含 `Calendar save requires an API endpoint in the next backend slice.`。
- `CalendarEventDrawer.vue` 的表单输入会进入草稿，并通过 `save` 事件传给 `CalendarView.vue`。
- 计划中列出的后端和前端验证命令退出码为 0 后，才允许提交实现。

## 自检记录

- 方案要求复用真实 v1 Calendar runtime：本计划通过 `V21CalendarController` 直接调用 `CalendarService` 和 `CalendarAvailabilityService`。
- 方案要求移除前端保存占位：本计划用契约测试锁定占位文案删除，并实现 create/update 后 `loadCalendar()`。
- 方案要求 Premium resources/bookings gate：本计划不新增资源/预订 controller，测试断言 gate 返回 `V2_ENTITLEMENT_REQUIRED`。
- Debug-first 要求禁止假成功：settings 只持久化已有 `timezone` 字段，对无存储字段的变更返回显式错误。
- 提交规范要求只暂存相关文件：本计划列出逐文件 `git add`，不使用 `git add .` 或 `git add -A`。
