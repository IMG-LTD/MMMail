package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateContactGroupRequest;
import com.mmmail.server.model.dto.UpdateContactGroupMembersRequest;
import com.mmmail.server.model.dto.UpdateContactGroupRequest;
import com.mmmail.server.model.vo.ContactGroupItemVo;
import com.mmmail.server.model.vo.ContactItemVo;
import com.mmmail.server.service.ContactGroupService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contact-groups")
public class ContactGroupController {

    private final ContactGroupService contactGroupService;

    public ContactGroupController(ContactGroupService contactGroupService) {
        this.contactGroupService = contactGroupService;
    }

    @GetMapping
    public Result<List<ContactGroupItemVo>> list() {
        return Result.success(contactGroupService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public Result<ContactGroupItemVo> create(
            @Valid @RequestBody CreateContactGroupRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactGroupService.create(
                SecurityUtils.currentUserId(),
                request.name(),
                request.description(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{groupId}")
    public Result<ContactGroupItemVo> update(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateContactGroupRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactGroupService.update(
                SecurityUtils.currentUserId(),
                groupId,
                request.name(),
                request.description(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{groupId}")
    public Result<Void> delete(@PathVariable Long groupId, HttpServletRequest httpRequest) {
        contactGroupService.delete(SecurityUtils.currentUserId(), groupId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/{groupId}/members")
    public Result<List<ContactItemVo>> listMembers(@PathVariable Long groupId) {
        return Result.success(contactGroupService.listMembers(SecurityUtils.currentUserId(), groupId));
    }

    @PostMapping("/{groupId}/members")
    public Result<List<ContactItemVo>> addMembers(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateContactGroupMembersRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(contactGroupService.addMembers(
                SecurityUtils.currentUserId(),
                groupId,
                request.contactIds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{groupId}/members/{contactId}")
    public Result<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long contactId,
            HttpServletRequest httpRequest
    ) {
        contactGroupService.removeMember(SecurityUtils.currentUserId(), groupId, contactId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }
}
