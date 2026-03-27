package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.AddPassSharedVaultMemberRequest;
import com.mmmail.server.model.dto.CreatePassAliasContactRequest;
import com.mmmail.server.model.dto.CreatePassAliasRequest;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.CreatePassMailboxRequest;
import com.mmmail.server.model.dto.GeneratePasswordRequest;
import com.mmmail.server.model.dto.UpsertPassItemTwoFactorRequest;
import com.mmmail.server.model.dto.UpdatePassAliasContactRequest;
import com.mmmail.server.model.dto.UpdatePassAliasRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.dto.VerifyPassMailboxRequest;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import com.mmmail.server.model.vo.GeneratedPasswordVo;
import com.mmmail.server.model.vo.PassAliasContactVo;
import com.mmmail.server.model.vo.PassItemDetailVo;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassMailboxVo;
import com.mmmail.server.model.vo.PassMailAliasVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import com.mmmail.server.service.PassAliasContactService;
import com.mmmail.server.service.PassAliasService;
import com.mmmail.server.service.PassMailboxService;
import com.mmmail.server.service.PassMonitorService;
import com.mmmail.server.service.PassService;
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
@RequestMapping("/api/v1/pass")
public class PassController {

    private final PassService passService;
    private final PassAliasService passAliasService;
    private final PassAliasContactService passAliasContactService;
    private final PassMailboxService passMailboxService;
    private final PassMonitorService passMonitorService;

    public PassController(
            PassService passService,
            PassAliasService passAliasService,
            PassAliasContactService passAliasContactService,
            PassMailboxService passMailboxService,
            PassMonitorService passMonitorService
    ) {
        this.passService = passService;
        this.passAliasService = passAliasService;
        this.passAliasContactService = passAliasContactService;
        this.passMailboxService = passMailboxService;
        this.passMonitorService = passMonitorService;
    }

    @GetMapping("/items")
    public Result<List<PassItemSummaryVo>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String itemType
    ) {
        return Result.success(passService.list(SecurityUtils.currentUserId(), keyword, favoriteOnly, limit, itemType));
    }

    @PostMapping("/items")
    public Result<PassItemDetailVo> create(
            @Valid @RequestBody CreatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passService.create(
                SecurityUtils.currentUserId(),
                request.title(),
                request.itemType(),
                request.website(),
                request.username(),
                request.secretCiphertext(),
                request.note(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/items/{itemId}")
    public Result<PassItemDetailVo> get(@PathVariable Long itemId) {
        return Result.success(passService.get(SecurityUtils.currentUserId(), itemId));
    }

    @PutMapping("/items/{itemId}")
    public Result<PassItemDetailVo> update(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passService.update(
                SecurityUtils.currentUserId(),
                itemId,
                request.title(),
                request.itemType(),
                request.website(),
                request.username(),
                request.secretCiphertext(),
                request.note(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<Void> delete(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        passService.delete(SecurityUtils.currentUserId(), itemId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/items/{itemId}/favorite")
    public Result<PassItemDetailVo> favorite(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        return Result.success(passService.favorite(SecurityUtils.currentUserId(), itemId, true, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/items/{itemId}/favorite")
    public Result<PassItemDetailVo> unFavorite(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        return Result.success(passService.favorite(SecurityUtils.currentUserId(), itemId, false, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/monitor")
    public Result<PassMonitorOverviewVo> getMonitor(HttpServletRequest httpRequest) {
        return Result.success(passMonitorService.getPersonalMonitor(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/items/{itemId}/monitor/exclude")
    public Result<Void> excludeFromMonitor(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        passMonitorService.setPersonalMonitorExcluded(SecurityUtils.currentUserId(), itemId, true, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @DeleteMapping("/items/{itemId}/monitor/exclude")
    public Result<Void> includeInMonitor(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        passMonitorService.setPersonalMonitorExcluded(SecurityUtils.currentUserId(), itemId, false, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PutMapping("/items/{itemId}/two-factor")
    public Result<PassItemDetailVo> upsertTwoFactor(
            @PathVariable Long itemId,
            @Valid @RequestBody UpsertPassItemTwoFactorRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passService.upsertTwoFactor(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/items/{itemId}/two-factor")
    public Result<PassItemDetailVo> deleteTwoFactor(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        return Result.success(passService.deleteTwoFactor(
                SecurityUtils.currentUserId(),
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/items/{itemId}/two-factor/code")
    public Result<AuthenticatorCodeVo> generateTwoFactorCode(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        return Result.success(passService.generateTwoFactorCode(
                SecurityUtils.currentUserId(),
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/mailboxes")
    public Result<List<PassMailboxVo>> listMailboxes(HttpServletRequest httpRequest) {
        return Result.success(passMailboxService.listMailboxes(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/mailboxes")
    public Result<PassMailboxVo> createMailbox(
            @Valid @RequestBody CreatePassMailboxRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passMailboxService.createMailbox(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/mailboxes/{mailboxId}/verify")
    public Result<PassMailboxVo> verifyMailbox(
            @PathVariable Long mailboxId,
            @Valid @RequestBody VerifyPassMailboxRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passMailboxService.verifyMailbox(SecurityUtils.currentUserId(), mailboxId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/mailboxes/{mailboxId}/default")
    public Result<PassMailboxVo> setDefaultMailbox(@PathVariable Long mailboxId, HttpServletRequest httpRequest) {
        return Result.success(passMailboxService.setDefaultMailbox(SecurityUtils.currentUserId(), mailboxId, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/mailboxes/{mailboxId}")
    public Result<Void> deleteMailbox(@PathVariable Long mailboxId, HttpServletRequest httpRequest) {
        passMailboxService.deleteMailbox(SecurityUtils.currentUserId(), mailboxId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/aliases")
    public Result<List<PassMailAliasVo>> listAliases(HttpServletRequest httpRequest) {
        return Result.success(passAliasService.listAliases(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/aliases")
    public Result<PassMailAliasVo> createAlias(
            @Valid @RequestBody CreatePassAliasRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passAliasService.createAlias(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/aliases/{aliasId}")
    public Result<PassMailAliasVo> updateAlias(
            @PathVariable Long aliasId,
            @Valid @RequestBody UpdatePassAliasRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passAliasService.updateAlias(SecurityUtils.currentUserId(), aliasId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/aliases/{aliasId}/enable")
    public Result<PassMailAliasVo> enableAlias(@PathVariable Long aliasId, HttpServletRequest httpRequest) {
        return Result.success(passAliasService.enableAlias(SecurityUtils.currentUserId(), aliasId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/aliases/{aliasId}/disable")
    public Result<PassMailAliasVo> disableAlias(@PathVariable Long aliasId, HttpServletRequest httpRequest) {
        return Result.success(passAliasService.disableAlias(SecurityUtils.currentUserId(), aliasId, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/aliases/{aliasId}")
    public Result<Void> deleteAlias(@PathVariable Long aliasId, HttpServletRequest httpRequest) {
        passAliasService.deleteAlias(SecurityUtils.currentUserId(), aliasId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/aliases/{aliasId}/contacts")
    public Result<List<PassAliasContactVo>> listAliasContacts(@PathVariable Long aliasId, HttpServletRequest httpRequest) {
        return Result.success(passAliasContactService.listContacts(SecurityUtils.currentUserId(), aliasId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/aliases/{aliasId}/contacts")
    public Result<PassAliasContactVo> createAliasContact(
            @PathVariable Long aliasId,
            @Valid @RequestBody CreatePassAliasContactRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passAliasContactService.createContact(SecurityUtils.currentUserId(), aliasId, request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/aliases/{aliasId}/contacts/{contactId}")
    public Result<PassAliasContactVo> updateAliasContact(
            @PathVariable Long aliasId,
            @PathVariable Long contactId,
            @Valid @RequestBody UpdatePassAliasContactRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passAliasContactService.updateContact(SecurityUtils.currentUserId(), aliasId, contactId, request, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/aliases/{aliasId}/contacts/{contactId}")
    public Result<Void> deleteAliasContact(
            @PathVariable Long aliasId,
            @PathVariable Long contactId,
            HttpServletRequest httpRequest
    ) {
        passAliasContactService.deleteContact(SecurityUtils.currentUserId(), aliasId, contactId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/alias-contacts/suggestions")
    public Result<List<PassAliasContactVo>> suggestAliasContacts(
            @RequestParam String senderEmail,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(passAliasContactService.suggestContacts(SecurityUtils.currentUserId(), senderEmail, keyword, limit));
    }

    @PostMapping("/password/generate")
    public Result<GeneratedPasswordVo> generate(
            @RequestBody(required = false) GeneratePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passService.generatePassword(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
