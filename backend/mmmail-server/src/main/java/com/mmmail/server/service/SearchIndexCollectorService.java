package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.mmmail.server.model.entity.SearchIndex;
import com.mmmail.server.model.entity.SheetsWorkbook;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SearchIndexCollectorService {
    private static final String WORKSPACE_DOCS = "DOCS";
    private static final String WORKSPACE_NOTES = "STANDARD_NOTES";
    private static final String COMMUNITY_PUBLISHED = "published";

    private final MailMessageMapper mailMessageMapper;
    private final DocsNoteMapper docsNoteMapper;
    private final SheetsWorkbookMapper sheetsWorkbookMapper;
    private final DriveItemMapper driveItemMapper;
    private final ContactEntryMapper contactEntryMapper;
    private final CommunityPostMapper communityPostMapper;

    public SearchIndexCollectorService(
            MailMessageMapper mailMessageMapper,
            DocsNoteMapper docsNoteMapper,
            SheetsWorkbookMapper sheetsWorkbookMapper,
            DriveItemMapper driveItemMapper,
            ContactEntryMapper contactEntryMapper,
            CommunityPostMapper communityPostMapper
    ) {
        this.mailMessageMapper = mailMessageMapper;
        this.docsNoteMapper = docsNoteMapper;
        this.sheetsWorkbookMapper = sheetsWorkbookMapper;
        this.driveItemMapper = driveItemMapper;
        this.contactEntryMapper = contactEntryMapper;
        this.communityPostMapper = communityPostMapper;
    }

    public List<SearchIndex> collect(Set<String> modules) {
        List<SearchIndex> rows = new ArrayList<>();
        if (modules.contains("mail")) collectMail(rows);
        if (modules.contains("doc")) collectDocs(rows, WORKSPACE_DOCS, "doc");
        if (modules.contains("note")) collectDocs(rows, WORKSPACE_NOTES, "note");
        if (modules.contains("sheet")) collectSheets(rows);
        if (modules.contains("drive")) collectDrive(rows);
        if (modules.contains("contact")) collectContacts(rows);
        if (modules.contains("community")) collectCommunity(rows);
        return rows;
    }

    private void collectMail(List<SearchIndex> rows) {
        mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                        .eq(MailMessage::getIsDraft, 0))
                .forEach(mail -> rows.add(fromMail(mail)));
    }

    private void collectDocs(List<SearchIndex> rows, String workspaceType, String moduleType) {
        docsNoteMapper.selectList(new LambdaQueryWrapper<DocsNote>()
                        .eq(DocsNote::getWorkspaceType, workspaceType))
                .forEach(note -> rows.add(fromNote(note, moduleType)));
    }

    private void collectSheets(List<SearchIndex> rows) {
        sheetsWorkbookMapper.selectList(new LambdaQueryWrapper<SheetsWorkbook>())
                .forEach(workbook -> rows.add(fromWorkbook(workbook)));
    }

    private void collectDrive(List<SearchIndex> rows) {
        driveItemMapper.selectList(new LambdaQueryWrapper<DriveItem>()
                        .isNull(DriveItem::getTrashedAt))
                .forEach(item -> rows.add(fromDrive(item)));
    }

    private void collectContacts(List<SearchIndex> rows) {
        contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>())
                .forEach(contact -> rows.add(fromContact(contact)));
    }

    private void collectCommunity(List<SearchIndex> rows) {
        communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getStatus, COMMUNITY_PUBLISHED))
                .forEach(post -> rows.add(fromCommunity(post)));
    }

    private SearchIndex fromMail(MailMessage mail) {
        SearchIndex row = base("mail", String.valueOf(mail.getId()));
        row.setOwnerUserId(mail.getOwnerId());
        row.setTitle(title(mail.getSubject(), "(no subject)"));
        row.setBody(mailBody(mail));
        row.setRoutePath("/mail/" + mail.getId());
        row.setUpdatedAt(defaultTime(mail.getUpdatedAt()));
        return row;
    }

    private SearchIndex fromNote(DocsNote note, String moduleType) {
        SearchIndex row = base(moduleType, String.valueOf(note.getId()));
        row.setOwnerUserId(note.getOwnerId());
        row.setTitle(title(note.getTitle(), "Untitled " + moduleType));
        row.setBody(note.getContent());
        row.setRoutePath(notePath(moduleType, note.getId()));
        row.setUpdatedAt(defaultTime(note.getUpdatedAt()));
        return row;
    }

    private SearchIndex fromWorkbook(SheetsWorkbook workbook) {
        SearchIndex row = base("sheet", String.valueOf(workbook.getId()));
        row.setOwnerUserId(workbook.getOwnerId());
        row.setTitle(title(workbook.getTitle(), "Untitled sheet"));
        row.setBody(compact(workbook.getGridJson(), workbook.getSheetsJson()));
        row.setRoutePath("/sheets?workbookId=" + workbook.getId());
        row.setUpdatedAt(defaultTime(workbook.getUpdatedAt()));
        return row;
    }

    private SearchIndex fromDrive(DriveItem item) {
        SearchIndex row = base("drive", String.valueOf(item.getId()));
        row.setOwnerUserId(item.getOwnerId());
        row.setTitle(title(item.getName(), "Untitled drive item"));
        row.setBody(compact(item.getMimeType(), item.getStoragePath()));
        row.setRoutePath("/drive?itemId=" + item.getId());
        row.setUpdatedAt(defaultTime(item.getUpdatedAt()));
        return row;
    }

    private SearchIndex fromContact(ContactEntry contact) {
        SearchIndex row = base("contact", String.valueOf(contact.getId()));
        row.setOwnerUserId(contact.getOwnerId());
        row.setTitle(title(contact.getDisplayName(), contact.getEmail()));
        row.setBody(compact(contact.getEmail(), contact.getNote()));
        row.setRoutePath("/contacts?contactId=" + contact.getId());
        row.setUpdatedAt(defaultTime(contact.getUpdatedAt()));
        return row;
    }

    private SearchIndex fromCommunity(CommunityPost post) {
        SearchIndex row = base("community", post.getId());
        row.setOrgId(post.getOrgId());
        row.setTitle(title(post.getTitle(), "Community post"));
        row.setBody(compact(post.getBodyMd(), post.getTagsJson()));
        row.setRoutePath("/community?postId=" + post.getId());
        row.setUpdatedAt(defaultTime(post.getUpdatedAt()));
        return row;
    }

    private SearchIndex base(String moduleType, String resourceId) {
        SearchIndex row = new SearchIndex();
        row.setModuleType(moduleType);
        row.setResourceId(resourceId);
        row.setAclUserIds("");
        row.setCreatedAt(LocalDateTime.now());
        row.setDeleted(0);
        return row;
    }

    private String mailBody(MailMessage mail) {
        if (Integer.valueOf(1).equals(mail.getBodyE2eeEnabled())) {
            return "";
        }
        return compact(mail.getPeerEmail(), mail.getSenderEmail(), mail.getBodyCiphertext());
    }

    private String compact(String... parts) {
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) values.add(part);
        }
        return String.join(" ", values);
    }

    private String notePath(String moduleType, Long noteId) {
        if ("doc".equals(moduleType)) {
            return "/docs?id=" + noteId;
        }
        return "/notes?noteId=" + noteId;
    }

    private LocalDateTime defaultTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }

    private String title(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback == null || fallback.isBlank() ? "Untitled" : fallback;
    }
}
