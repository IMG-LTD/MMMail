package com.mmmail.server;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.mapper.CommunityPostMapper;
import com.mmmail.server.mapper.ContactEntryMapper;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.model.entity.CommunityPost;
import com.mmmail.server.model.entity.ContactEntry;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.SheetsWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MailMessageMapper mailMessageMapper;
    @Autowired
    private DocsNoteMapper docsNoteMapper;
    @Autowired
    private SheetsWorkbookMapper sheetsWorkbookMapper;
    @Autowired
    private DriveItemMapper driveItemMapper;
    @Autowired
    private ContactEntryMapper contactEntryMapper;
    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Test
    void searchShouldReindexAndReadAcrossIndexedModulesWithFacetsAndSuggestions() throws Exception {
        String suffix = "v212-search-" + System.nanoTime();
        AuthSession session = register(suffix + "@mmmail.local", "Search Owner");
        seedIndexedResources(session.userId(), suffix);

        String adminToken = login("admin@mmmail.local");
        String jobId = startReindex(adminToken, "all");
        mockMvc.perform(get("/api/v1/search/reindex/" + jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("succeeded"));

        MvcResult result = mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + session.token())
                        .param("q", suffix)
                        .param("types", "mail,doc,sheet,drive,contact,note,community")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(7))
                .andExpect(jsonPath("$.data.facets.byType.mail").value(1))
                .andReturn();

        Set<String> moduleTypes = readJson(result).at("/data/items").findValues("moduleType").stream()
                .map(JsonNode::asText)
                .collect(Collectors.toSet());
        assertThat(moduleTypes).contains("mail", "doc", "sheet", "drive", "contact", "note", "community");

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .header("Authorization", "Bearer " + session.token())
                        .param("q", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").exists());
        mockMvc.perform(get("/api/v1/search/facets")
                        .header("Authorization", "Bearer " + session.token())
                        .param("q", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.byType.community").value(1));
    }

    @Test
    void searchShouldRejectShortQueriesAndFilterPrivateIndexedRows() throws Exception {
        String suffix = "private-search-" + System.nanoTime();
        AuthSession owner = register("owner-" + suffix + "@mmmail.local", "Owner");
        AuthSession outsider = register("outsider-" + suffix + "@mmmail.local", "Outsider");
        insertMail(owner.userId(), suffix);
        startReindex(login("admin@mmmail.local"), "mail");

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + owner.token())
                        .param("q", "a"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + outsider.token())
                        .param("q", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    private void seedIndexedResources(Long ownerId, String suffix) {
        insertMail(ownerId, suffix);
        insertDocs(ownerId, suffix, "DOCS");
        insertDocs(ownerId, suffix, "STANDARD_NOTES");
        insertSheet(ownerId, suffix);
        insertDrive(ownerId, suffix);
        insertContact(ownerId, suffix);
        insertCommunity(ownerId, suffix);
    }

    private void insertMail(Long ownerId, String suffix) {
        MailMessage mail = new MailMessage();
        mail.setId(IdWorker.getId());
        mail.setOwnerId(ownerId);
        mail.setPeerEmail("search-peer@mmmail.local");
        mail.setDirection("IN");
        mail.setFolderType("INBOX");
        mail.setSubject("Mail " + suffix);
        mail.setBodyCiphertext("Search body " + suffix);
        mail.setBodyE2eeEnabled(0);
        mail.setIsRead(0);
        mail.setIsStarred(0);
        mail.setIsDraft(0);
        mail.setLabelsJson("[]");
        mail.setIdempotencyKey("search-" + mail.getId());
        stampMail(mail);
        mailMessageMapper.insert(mail);
    }

    private void insertDocs(Long ownerId, String suffix, String workspaceType) {
        DocsNote note = new DocsNote();
        note.setId(IdWorker.getId());
        note.setOwnerId(ownerId);
        note.setWorkspaceType(workspaceType);
        note.setTitle(workspaceType + " " + suffix);
        note.setContent("Search content " + suffix);
        note.setCurrentVersion(1);
        stampNote(note);
        docsNoteMapper.insert(note);
    }

    private void insertSheet(Long ownerId, String suffix) {
        SheetsWorkbook workbook = new SheetsWorkbook();
        workbook.setId(IdWorker.getId());
        workbook.setOwnerId(ownerId);
        workbook.setTitle("Sheet " + suffix);
        workbook.setRowCount(3);
        workbook.setColCount(3);
        workbook.setGridJson("{\"keyword\":\"" + suffix + "\"}");
        workbook.setSheetsJson("[]");
        workbook.setActiveSheetId("sheet-1");
        workbook.setCurrentVersion(1);
        stampWorkbook(workbook);
        sheetsWorkbookMapper.insert(workbook);
    }

    private void insertDrive(Long ownerId, String suffix) {
        DriveItem item = new DriveItem();
        item.setId(IdWorker.getId());
        item.setOwnerId(ownerId);
        item.setItemType("FILE");
        item.setName("Drive " + suffix);
        item.setMimeType("text/plain");
        item.setSizeBytes(16L);
        item.setE2eeEnabled(0);
        stampDrive(item);
        driveItemMapper.insert(item);
    }

    private void insertContact(Long ownerId, String suffix) {
        ContactEntry contact = new ContactEntry();
        contact.setId(IdWorker.getId());
        contact.setOwnerId(ownerId);
        contact.setDisplayName("Contact " + suffix);
        contact.setEmail(suffix + "@example.test");
        contact.setNote("Search note");
        contact.setIsFavorite(0);
        stampContact(contact);
        contactEntryMapper.insert(contact);
    }

    private void insertCommunity(Long ownerId, String suffix) {
        CommunityPost post = new CommunityPost();
        post.setId("ps_" + IdWorker.getIdStr());
        post.setAuthorUserId(ownerId);
        post.setTopicId("tp_general");
        post.setTitle("Community " + suffix);
        post.setBodyMd("Community body " + suffix);
        post.setBodyHtml("<p>Community body</p>");
        post.setTagsJson("[\"search\"]");
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setPinned(0);
        post.setLocked(0);
        post.setStatus("published");
        stampCommunity(post);
        communityPostMapper.insert(post);
    }

    private void stampMail(MailMessage mail) {
        LocalDateTime now = LocalDateTime.now();
        mail.setSentAt(now);
        mail.setCreatedAt(now);
        mail.setUpdatedAt(now);
        mail.setDeleted(0);
    }

    private void stampNote(DocsNote note) {
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        note.setDeleted(0);
    }

    private void stampWorkbook(SheetsWorkbook workbook) {
        LocalDateTime now = LocalDateTime.now();
        workbook.setLastOpenedAt(now);
        workbook.setCreatedAt(now);
        workbook.setUpdatedAt(now);
        workbook.setDeleted(0);
    }

    private void stampDrive(DriveItem item) {
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
    }

    private void stampContact(ContactEntry contact) {
        LocalDateTime now = LocalDateTime.now();
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);
        contact.setDeleted(0);
    }

    private void stampCommunity(CommunityPost post) {
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        post.setDeleted(0);
    }

    private String startReindex(String adminToken, String moduleType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/search/reindex/" + moduleType)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/jobId").asText();
    }

    private AuthSession register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"%s"}
                                """.formatted(email, PASSWORD, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = readJson(result);
        return new AuthSession(json.at("/data/accessToken").asText(), json.at("/data/user/id").asLong());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record AuthSession(String token, Long userId) {
    }
}
