# Backend v2.1 Mail Runtime Bridge 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 v2.1 Mail 增加真实 `/api/v2/mail/*` 运行时桥接，让前端 Mail 工作台走后端真实邮件状态。

**架构：** 新增 `V21MailController` 作为薄适配层，复用现有 `MailService`、`PublicBaseUrlResolver`、Mail DTO/VO 和 v2 access gate。新增最小 v2 DTO/VO 只处理前端 v2 字段差异，例如 `messageIds` 到 `mailIds` 的转换、folder query 绑定和 folder summary 输出。

**技术栈：** Java 21、Spring Boot 3.5、MockMvc、JUnit 5、AssertJ、Vue 3 frontend-v2 contract tests、Maven、pnpm。

**执行状态：** completed on 2026-05-13.

**实现提交：** `4730afdc feat(backend-v21): add mail runtime bridge`

**实际验证证据：**
- `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest,MailAttachmentIntegrationTest,SmtpOutboundDeliveryIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS (`23/23`)
- `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS (`4/4`, post-review import cleanup)
- `pnpm --dir frontend-v2 test`: PASS (`84/84`)
- `pnpm --dir frontend-v2 typecheck`: PASS
- `pnpm --dir frontend-v2 build`: PASS

**执行偏差记录：** 最终实现比原计划多覆盖两项审查风险：`bulk-action` 在变更前先校验全部 `messageIds`，避免无效 ID 造成部分写入；`HttpMessageNotReadableException` 统一映射为 `INVALID_ARGUMENT`，避免 malformed JSON 进入 500。前端同步补充 `SendMailPayload.draftId` 类型以匹配真实发送草稿流程。

---

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21MailRuntimeBridgeTest.java`
  - v2 Mail runtime bridge 红绿测试，覆盖真实草稿、发送、读取、联系人、recipient trust、批量动作和 Premium gate。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailMessagesQuery.java`
  - 绑定 `/api/v2/mail/messages` 的 query 参数，避免 controller 方法参数过多。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailBulkActionRequest.java`
  - 将前端 v2 `messageIds` 转成 v1 `BatchMailActionRequest.mailIds`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21MailFolderVo.java`
  - 输出 v2 Mail folder rail 所需的 `key`、`label`、`unreadCount`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21MailController.java`
  - `/api/v2/mail` controller，复用真实 Mail runtime。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  - 实现完成后记录切片、提交号和验证证据。

## 任务 1：新增 v2 Mail runtime 红测

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21MailRuntimeBridgeTest.java`
- 测试：`BackendV21MailRuntimeBridgeTest`

- [x] **步骤 1：创建失败测试文件**

写入 `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21MailRuntimeBridgeTest.java`：

```java
package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21MailRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21MailShouldUseRuntimeDraftSendAndFolderState() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "v21-mail-sender-" + suffix + "@mmmail.local";
        String receiverEmail = "v21-mail-receiver-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "V21 Mail Sender");
        String receiverToken = register(receiverEmail, "V21 Mail Receiver");
        setUndoSendSeconds(senderToken, "V21 Mail Sender", 0);

        String draftId = createDraft(senderToken, receiverEmail, "V21 Draft", "Initial v2 body");
        patchDraft(senderToken, draftId, receiverEmail);
        sendDraft(senderToken, draftId, receiverEmail, suffix);

        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("folder", "sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject").value("V21 Mail Runtime"))
                .andExpect(jsonPath("$.data.items[0].peerEmail").value(receiverEmail));

        MvcResult inbox = mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + receiverToken)
                        .param("folder", "inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject").value("V21 Mail Runtime"))
                .andReturn();
        String receivedMailId = readJson(inbox).at("/data/items/0/id").asText();

        mockMvc.perform(get("/api/v2/mail/threads/" + receivedMailId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(receivedMailId))
                .andExpect(jsonPath("$.data.body").value("Updated v2 body"));
    }

    @Test
    void v21MailShouldExposeContactsRecipientTrustBulkActionAndGates() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "v21-mail-contact-" + suffix + "@mmmail.local";
        String receiverEmail = "v21-mail-bulk-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "V21 Mail Contact");
        String receiverToken = register(receiverEmail, "V21 Mail Bulk");
        setUndoSendSeconds(senderToken, "V21 Mail Contact", 0);
        String draftId = createDraft(senderToken, receiverEmail, "Bulk Target", "Bulk body");
        sendDraft(senderToken, draftId, receiverEmail, suffix);
        String receivedMailId = latestInboxMailId(receiverToken);

        mockMvc.perform(get("/api/v2/mail/contacts")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].emailAddress").value(senderEmail));

        mockMvc.perform(get("/api/v2/mail/contacts")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("capability", "recipient-trust")
                        .param("toEmail", receiverEmail)
                        .param("fromEmail", senderEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliverable").value(true))
                .andExpect(jsonPath("$.data.routeCount").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/v2/mail/messages/bulk-action")
                        .header("Authorization", "Bearer " + receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageIds": ["%s"],
                                  "action": "MARK_READ"
                                }
                                """.formatted(receivedMailId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));

        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + receiverToken)
                        .param("folder", "missing-folder"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        mockMvc.perform(get("/api/v2/mail/rules")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private String createDraft(String token, String toEmail, String subject, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/mail/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s"
                                }
                                """.formatted(toEmail, subject, body)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void patchDraft(String token, String draftId, String toEmail) throws Exception {
        mockMvc.perform(patch("/api/v2/mail/drafts/" + draftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "V21 Mail Runtime",
                                  "body": "Updated v2 body"
                                }
                                """.formatted(toEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(draftId))
                .andExpect(jsonPath("$.data.subject").value("V21 Mail Runtime"));
    }

    private void sendDraft(String token, String draftId, String toEmail, String suffix) throws Exception {
        mockMvc.perform(post("/api/v2/mail/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %s,
                                  "toEmail": "%s",
                                  "subject": "V21 Mail Runtime",
                                  "body": "Updated v2 body",
                                  "idempotencyKey": "v21-mail-%s",
                                  "labels": []
                                }
                                """.formatted(draftId, toEmail, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String latestInboxMailId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "inbox"))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/items/0/id").asText();
    }

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "signature": "",
                                  "timezone": "UTC",
                                  "autoSaveSeconds": 15,
                                  "undoSendSeconds": %d,
                                  "driveVersionRetentionCount": 50,
                                  "driveVersionRetentionDays": 365
                                }
                                """.formatted(displayName, undoSeconds)))
                .andExpect(status().isOk());
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
}
```

- [x] **步骤 2：运行红测并确认失败**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。失败原因应指向 `/api/v2/mail/drafts`、`/api/v2/mail/messages` 或 `/api/v2/mail/contacts` 还没有 controller/runtime handler，而不是测试编译错误。

## 任务 2：新增 v2 Mail 请求和响应适配类型

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailMessagesQuery.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailBulkActionRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21MailFolderVo.java`

- [x] **步骤 1：创建 messages query 绑定类**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailMessagesQuery.java`：

```java
package com.mmmail.server.model.dto;

public class V21MailMessagesQuery {

    private String folder = "inbox";
    private long page = 1;
    private long size = 20;
    private String keyword = "";
    private Boolean unread;
    private Boolean needsReply;
    private Boolean starred;
    private Boolean hasAttachments;
    private Boolean importantContact;

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getUnread() {
        return unread;
    }

    public void setUnread(Boolean unread) {
        this.unread = unread;
    }

    public Boolean getNeedsReply() {
        return needsReply;
    }

    public void setNeedsReply(Boolean needsReply) {
        this.needsReply = needsReply;
    }

    public Boolean getStarred() {
        return starred;
    }

    public void setStarred(Boolean starred) {
        this.starred = starred;
    }

    public Boolean getHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(Boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public Boolean getImportantContact() {
        return importantContact;
    }

    public void setImportantContact(Boolean importantContact) {
        this.importantContact = importantContact;
    }
}
```

- [x] **步骤 2：创建 bulk-action v2 请求 record**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailBulkActionRequest.java`：

```java
package com.mmmail.server.model.dto;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record V21MailBulkActionRequest(
        @NotEmpty List<String> messageIds,
        @NotBlank String action
) {
    public BatchMailActionRequest toBatchRequest() {
        return new BatchMailActionRequest(messageIds.stream().map(V21MailBulkActionRequest::parseId).toList(), action);
    }

    private static Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid mail message id");
        }
    }
}
```

- [x] **步骤 3：创建 folder 响应 record**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21MailFolderVo.java`：

```java
package com.mmmail.server.model.vo;

public record V21MailFolderVo(
        String key,
        String label,
        long unreadCount
) {
}
```

- [x] **步骤 4：运行编译范围测试验证新增类型可编译**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

## 任务 3：实现 `V21MailController`

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21MailController.java`
- 测试：`BackendV21MailRuntimeBridgeTest`

- [x] **步骤 1：创建 controller 并接入真实 MailService**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21MailController.java`：

```java
package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.SaveDraftRequest;
import com.mmmail.server.model.dto.SendMailRequest;
import com.mmmail.server.model.dto.V21MailBulkActionRequest;
import com.mmmail.server.model.dto.V21MailMessagesQuery;
import com.mmmail.server.model.vo.DraftSaveVo;
import com.mmmail.server.model.vo.MailActionResultVo;
import com.mmmail.server.model.vo.MailDetailVo;
import com.mmmail.server.model.vo.MailPageVo;
import com.mmmail.server.model.vo.MailboxStatsVo;
import com.mmmail.server.model.vo.V21MailFolderVo;
import com.mmmail.server.service.MailService;
import com.mmmail.server.service.PublicBaseUrlResolver;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v2/mail")
public class V21MailController {

    private static final List<FolderDescriptor> FOLDERS = List.of(
            new FolderDescriptor("inbox", "Inbox", "INBOX"),
            new FolderDescriptor("unread", "Unread", "INBOX"),
            new FolderDescriptor("starred", "Starred", null),
            new FolderDescriptor("drafts", "Drafts", "DRAFTS"),
            new FolderDescriptor("scheduled", "Scheduled", "SCHEDULED"),
            new FolderDescriptor("outbox", "Outbox", "OUTBOX"),
            new FolderDescriptor("sent", "Sent", "SENT"),
            new FolderDescriptor("archive", "Archive", "ARCHIVE"),
            new FolderDescriptor("spam", "Spam", "SPAM"),
            new FolderDescriptor("trash", "Trash", "TRASH"),
            new FolderDescriptor("snoozed", "Snoozed", "SNOOZED")
    );
    private static final Map<String, String> SERVICE_FOLDERS = Map.of(
            "sent", "SENT",
            "drafts", "DRAFTS",
            "archive", "ARCHIVE",
            "spam", "SPAM",
            "trash", "TRASH",
            "outbox", "OUTBOX",
            "scheduled", "SCHEDULED",
            "snoozed", "SNOOZED"
    );

    private final MailService mailService;
    private final PublicBaseUrlResolver publicBaseUrlResolver;

    public V21MailController(MailService mailService, PublicBaseUrlResolver publicBaseUrlResolver) {
        this.mailService = mailService;
        this.publicBaseUrlResolver = publicBaseUrlResolver;
    }

    @GetMapping("/folders")
    public Result<List<V21MailFolderVo>> folders() {
        MailboxStatsVo stats = mailService.stats(SecurityUtils.currentUserId());
        return Result.success(FOLDERS.stream().map(folder -> toFolderVo(folder, stats)).toList());
    }

    @GetMapping("/messages")
    public Result<MailPageVo> messages(@ModelAttribute V21MailMessagesQuery query) {
        return Result.success(listMessages(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/threads/{mailId}")
    public Result<MailDetailVo> thread(@PathVariable String mailId) {
        return Result.success(mailService.detail(SecurityUtils.currentUserId(), parseId(mailId, "mail id")));
    }

    @PostMapping("/drafts")
    public Result<MailDetailVo> createDraft(@Valid @RequestBody SaveDraftRequest request, HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        DraftSaveVo saved = mailService.saveDraft(userId, request, httpRequest.getRemoteAddr());
        return Result.success(mailService.detail(userId, Long.valueOf(saved.draftId())));
    }

    @PatchMapping("/drafts/{draftId}")
    public Result<MailDetailVo> updateDraft(
            @PathVariable Long draftId,
            @Valid @RequestBody SaveDraftRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        DraftSaveVo saved = mailService.saveDraft(userId, withDraftId(draftId, request), httpRequest.getRemoteAddr());
        return Result.success(mailService.detail(userId, Long.valueOf(saved.draftId())));
    }

    @PostMapping("/send")
    public Result<Void> send(@Valid @RequestBody SendMailRequest request, HttpServletRequest httpRequest) {
        mailService.send(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr(),
                publicBaseUrlResolver.resolve(httpRequest)
        );
        return Result.success(null);
    }

    @PostMapping("/messages/bulk-action")
    public Result<MailActionResultVo> bulkAction(
            @Valid @RequestBody V21MailBulkActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.applyBatchAction(
                SecurityUtils.currentUserId(),
                request.toBatchRequest(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/contacts")
    public Result<?> contacts(
            @RequestParam(required = false) String capability,
            @RequestParam(required = false) @Email @NotBlank String toEmail,
            @RequestParam(required = false) @Email String fromEmail,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        if ("recipient-trust".equals(capability)) {
            return Result.success(mailService.previewRecipientE2eeStatus(userId, toEmail, fromEmail));
        }
        return Result.success(mailService.listSenderIdentities(userId, httpRequest.getRemoteAddr()));
    }

    private MailPageVo listMessages(Long userId, V21MailMessagesQuery query) {
        String folder = normalizeFolder(query.getFolder());
        return switch (folder) {
            case "inbox" -> mailService.listInbox(userId, query.getPage(), query.getSize(), query.getKeyword(), toFilters(query));
            case "unread" -> mailService.listUnread(userId, query.getPage(), query.getSize(), query.getKeyword());
            case "starred" -> mailService.listStarred(userId, query.getPage(), query.getSize(), query.getKeyword());
            case "search" -> mailService.search(userId, query.getKeyword(), null, query.getUnread(), query.getStarred(), null, null, null, query.getPage(), query.getSize());
            default -> mailService.listFolder(userId, serviceFolder(folder), query.getPage(), query.getSize(), query.getKeyword());
        };
    }

    private MailService.InboxTriageFilters toFilters(V21MailMessagesQuery query) {
        return new MailService.InboxTriageFilters(
                query.getUnread(),
                query.getNeedsReply(),
                query.getStarred(),
                query.getHasAttachments(),
                query.getImportantContact()
        );
    }

    private String normalizeFolder(String folder) {
        return StringUtils.hasText(folder) ? folder.toLowerCase(Locale.ROOT) : "inbox";
    }

    private String serviceFolder(String folder) {
        String serviceFolder = SERVICE_FOLDERS.get(folder);
        if (serviceFolder == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported v2 mail folder: " + folder);
        }
        return serviceFolder;
    }

    private V21MailFolderVo toFolderVo(FolderDescriptor folder, MailboxStatsVo stats) {
        long unreadCount = "unread".equals(folder.key()) || "inbox".equals(folder.key())
                ? stats.unreadCount()
                : 0;
        return new V21MailFolderVo(folder.key(), folder.label(), unreadCount);
    }

    private SaveDraftRequest withDraftId(Long draftId, SaveDraftRequest request) {
        if (request.draftId() != null && !request.draftId().equals(draftId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Draft id path and body do not match");
        }
        return new SaveDraftRequest(draftId, request.toEmail(), request.fromEmail(), request.subject(), request.body(), request.e2ee());
    }

    private Long parseId(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid " + fieldName);
        }
    }

    private record FolderDescriptor(String key, String label, String serviceFolder) {
    }
}
```

- [x] **步骤 2：运行 v2 Mail 红测确认转绿**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS，`BackendV21MailRuntimeBridgeTest` 覆盖的测试全部通过。

- [x] **步骤 3：运行 v1 Mail 回归**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=MailAttachmentIntegrationTest,SmtpOutboundDeliveryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。v1 附件与 SMTP outbound 行为不回归。

- [x] **步骤 4：运行 v2 gate/catalog 回归**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。`/api/v2/mail/rules` 的 Premium gate 和 catalog metadata 不回归。

## 任务 4：前端合同与构建回归

**文件：**
- 测试：`frontend-v2/tests/mail-workspace-contract.test.mjs`
- 测试：`frontend-v2/tests/v21-core-workspaces-contract.test.mjs`
- 测试：`frontend-v2` 全量测试、类型检查、构建

- [x] **步骤 1：运行 frontend-v2 测试**

运行：

```bash
pnpm --dir frontend-v2 test
```

预期：PASS，包含 Mail v2 API contract 测试。

- [x] **步骤 2：运行 frontend-v2 typecheck**

运行：

```bash
pnpm --dir frontend-v2 typecheck
```

预期：PASS。

- [x] **步骤 3：运行 frontend-v2 build**

运行：

```bash
pnpm --dir frontend-v2 build
```

预期：PASS。

## 任务 5：提交 Mail runtime bridge 实现

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21MailRuntimeBridgeTest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailMessagesQuery.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailBulkActionRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21MailFolderVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21MailController.java`
- 修改：`backend/mmmail-common/src/main/java/com/mmmail/common/exception/GlobalExceptionHandler.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/service/MailService.java`
- 修改：`frontend-v2/src/service/api/mail.ts`

- [x] **步骤 1：检查工作树**

运行：

```bash
git status --short --branch
```

预期：只看到本任务相关源码和测试文件，外加既有无关未跟踪路径。

- [x] **步骤 2：暂存本任务相关文件**

运行：

```bash
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21MailRuntimeBridgeTest.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailMessagesQuery.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21MailBulkActionRequest.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21MailFolderVo.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21MailController.java
git diff --cached --check
git diff --cached --stat
```

预期：`git diff --cached --check` 无输出，stat 只包含本任务相关源码、测试和前端类型文件。

- [x] **步骤 3：提交实现**

运行：

```bash
git commit -m "feat(backend-v21): add mail runtime bridge"
```

预期：提交成功，提交内容只包含 Mail v2 runtime bridge 源码和测试。

## 任务 6：更新 v2.1 进度记录

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-mail-runtime-bridge.md`

- [x] **步骤 1：更新完成切片表**

在 `docs/superpowers/progress/v21-implementation-progress.md` 的 `Completed v2.1 Slices` 表中新增：

```markdown
| Backend Mail runtime bridge (`backend-v21-mail-runtime-bridge`) | `BackendV21MailRuntimeBridgeTest`, `V21MailController` |
```

- [x] **步骤 2：更新 Latest Completed Backend Slice**

将 `Latest Completed Backend Slice` 改成：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-mail-runtime-bridge`
- Commit: `4730afdc feat(backend-v21): add mail runtime bridge`
- Files changed: added v2 Mail controller, v2 Mail query/bulk-action/folder adapters, runtime bridge coverage for draft, send, folders, detail, contacts, recipient trust, batch action, unknown folder, and Premium mail rule gate.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest,MailAttachmentIntegrationTest,SmtpOutboundDeliveryIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `pnpm --dir frontend-v2 test`: PASS
  - `pnpm --dir frontend-v2 typecheck`: PASS
  - `pnpm --dir frontend-v2 build`: PASS
```

已替换为任务 5 产生的实际提交号。

- [x] **步骤 3：更新 Active Backend Slice**

将 `Active Backend Slice` 改成：

```markdown
## Active Backend Slice

- Slice: `backend-v21-mail-runtime-bridge`
- Status: `completed`
- Started: `2026-05-13`
- Completed: `2026-05-13`
- Scope: v2 Mail runtime bridge for drafts, send, folders, detail, contacts, recipient trust, batch action, unknown folder, and Premium rule gate
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest,MailAttachmentIntegrationTest,SmtpOutboundDeliveryIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `pnpm --dir frontend-v2 build`
```

- [x] **步骤 4：运行最终聚合验证**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21MailRuntimeBridgeTest,MailAttachmentIntegrationTest,SmtpOutboundDeliveryIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

预期：全部 PASS。

- [x] **步骤 5：提交进度文档**

运行：

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update mail runtime bridge progress"
```

预期：提交成功，提交内容只包含进度文档。

## 任务 7：收尾核对

**文件：**
- 测试：git 状态和最近提交

- [x] **步骤 1：确认最新提交**

运行：

```bash
git log --oneline -5
git status --short --branch
```

预期：最新提交包含：

```text
docs(backend-v21): update mail runtime bridge progress
feat(backend-v21): add mail runtime bridge
```

工作树只保留既有无关未跟踪路径，不出现本切片源码或文档未提交改动。

## 计划自检

- 规格覆盖度：本计划覆盖设计规格中的 controller、query adapter、bulk-action adapter、folder response、真实 runtime、未知 folder、Premium rules gate、v1 Mail 回归、frontend-v2 回归、进度记录。
- 占位符扫描：计划中未发现禁用占位标记、空章节或未定义任务。
- 类型一致性：`V21MailBulkActionRequest.messageIds` 映射到 `BatchMailActionRequest.mailIds`；`PATCH /drafts/{id}` 使用 path id 生成新的 `SaveDraftRequest`；`GET /threads/{id}` 明确按 mail id 调用 `MailService.detail`，匹配当前前端 `readMailDetail` 行为。
