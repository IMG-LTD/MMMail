package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateContactRequest;
import com.mmmail.server.model.dto.ImportContactsCsvRequest;
import com.mmmail.server.model.dto.MergeDuplicateContactsRequest;
import com.mmmail.server.model.dto.QuickAddContactRequest;
import com.mmmail.server.model.dto.UpdateContactRequest;
import com.mmmail.server.model.vo.ContactDuplicateGroupVo;
import com.mmmail.server.model.vo.ContactImportResultVo;
import com.mmmail.server.model.vo.ContactItemVo;
import com.mmmail.server.model.vo.ContactSuggestionVo;
import com.mmmail.server.service.ContactService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public Result<List<ContactItemVo>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Boolean favoriteOnly
    ) {
        return Result.success(contactService.list(SecurityUtils.currentUserId(), keyword, favoriteOnly));
    }

    @PostMapping
    public Result<ContactItemVo> create(@Valid @RequestBody CreateContactRequest request, HttpServletRequest httpRequest) {
        return Result.success(contactService.create(
                SecurityUtils.currentUserId(),
                request.displayName(),
                request.email(),
                request.note(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{contactId}")
    public Result<ContactItemVo> update(
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateContactRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactService.update(
                SecurityUtils.currentUserId(),
                contactId,
                request.displayName(),
                request.email(),
                request.note(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{contactId}")
    public Result<Void> delete(@PathVariable Long contactId, HttpServletRequest httpRequest) {
        contactService.delete(SecurityUtils.currentUserId(), contactId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/{contactId}/favorite")
    public Result<ContactItemVo> favorite(@PathVariable Long contactId, HttpServletRequest httpRequest) {
        return Result.success(contactService.favorite(SecurityUtils.currentUserId(), contactId, true, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{contactId}/unfavorite")
    public Result<ContactItemVo> unfavorite(@PathVariable Long contactId, HttpServletRequest httpRequest) {
        return Result.success(contactService.favorite(SecurityUtils.currentUserId(), contactId, false, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/quick-add")
    public Result<ContactItemVo> quickAdd(@Valid @RequestBody QuickAddContactRequest request, HttpServletRequest httpRequest) {
        return Result.success(contactService.quickAdd(
                SecurityUtils.currentUserId(),
                request.email(),
                request.displayName(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/suggestions")
    public Result<List<ContactSuggestionVo>> suggestions(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(contactService.suggestions(SecurityUtils.currentUserId(), keyword, limit));
    }

    @PostMapping("/import/csv")
    public Result<ContactImportResultVo> importCsv(
            @Valid @RequestBody ImportContactsCsvRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactService.importCsv(
                SecurityUtils.currentUserId(),
                request.content(),
                request.mergeDuplicates(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/export")
    public Result<String> exportContacts(
            @RequestParam(defaultValue = "csv") String format,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactService.exportContacts(
                SecurityUtils.currentUserId(),
                format,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/duplicates")
    public Result<List<ContactDuplicateGroupVo>> duplicates() {
        return Result.success(contactService.listDuplicates(SecurityUtils.currentUserId()));
    }

    @PostMapping("/duplicates/merge")
    public Result<ContactItemVo> mergeDuplicates(
            @Valid @RequestBody MergeDuplicateContactsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactService.mergeDuplicates(
                SecurityUtils.currentUserId(),
                request.primaryContactId(),
                request.duplicateContactIds(),
                httpRequest.getRemoteAddr()
        ));
    }
}
