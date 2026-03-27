package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.ContactEntryMapper;
import com.mmmail.server.mapper.ContactGroupMapper;
import com.mmmail.server.mapper.ContactGroupMemberMapper;
import com.mmmail.server.model.entity.ContactEntry;
import com.mmmail.server.model.entity.ContactGroup;
import com.mmmail.server.model.entity.ContactGroupMember;
import com.mmmail.server.model.vo.ContactGroupItemVo;
import com.mmmail.server.model.vo.ContactItemVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ContactGroupService {

    private final ContactGroupMapper contactGroupMapper;
    private final ContactGroupMemberMapper contactGroupMemberMapper;
    private final ContactEntryMapper contactEntryMapper;
    private final AuditService auditService;

    public ContactGroupService(
            ContactGroupMapper contactGroupMapper,
            ContactGroupMemberMapper contactGroupMemberMapper,
            ContactEntryMapper contactEntryMapper,
            AuditService auditService
    ) {
        this.contactGroupMapper = contactGroupMapper;
        this.contactGroupMemberMapper = contactGroupMemberMapper;
        this.contactEntryMapper = contactEntryMapper;
        this.auditService = auditService;
    }

    public List<ContactGroupItemVo> list(Long userId) {
        List<ContactGroup> groups = contactGroupMapper.selectList(new LambdaQueryWrapper<ContactGroup>()
                .eq(ContactGroup::getOwnerId, userId)
                .orderByDesc(ContactGroup::getUpdatedAt)
                .orderByAsc(ContactGroup::getName));

        Map<Long, Integer> memberCountMap = buildMemberCountMap(userId, groups);
        return groups.stream().map(group -> toGroupVo(group, memberCountMap.getOrDefault(group.getId(), 0))).toList();
    }

    @Transactional
    public ContactGroupItemVo create(Long userId, String name, String description, String ipAddress) {
        String normalizedName = requireGroupName(name);
        ensureGroupNameUnique(userId, normalizedName, null);

        ContactGroup group = new ContactGroup();
        LocalDateTime now = LocalDateTime.now();
        group.setOwnerId(userId);
        group.setName(normalizedName);
        group.setDescription(normalizeDescription(description));
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        group.setDeleted(0);
        contactGroupMapper.insert(group);

        auditService.record(userId, "CONTACT_GROUP_CREATE", "group=" + normalizedName, ipAddress);
        return toGroupVo(group, 0);
    }

    @Transactional
    public ContactGroupItemVo update(Long userId, Long groupId, String name, String description, String ipAddress) {
        ContactGroup group = loadGroup(userId, groupId);
        String normalizedName = requireGroupName(name);
        ensureGroupNameUnique(userId, normalizedName, groupId);

        group.setName(normalizedName);
        group.setDescription(normalizeDescription(description));
        group.setUpdatedAt(LocalDateTime.now());
        contactGroupMapper.updateById(group);

        int memberCount = countMembers(userId, groupId);
        auditService.record(userId, "CONTACT_GROUP_UPDATE", "groupId=" + groupId, ipAddress);
        return toGroupVo(group, memberCount);
    }

    @Transactional
    public void delete(Long userId, Long groupId, String ipAddress) {
        ContactGroup group = loadGroup(userId, groupId);
        contactGroupMemberMapper.delete(new LambdaQueryWrapper<ContactGroupMember>()
                .eq(ContactGroupMember::getOwnerId, userId)
                .eq(ContactGroupMember::getGroupId, groupId));
        contactGroupMapper.deleteById(group.getId());
        auditService.record(userId, "CONTACT_GROUP_DELETE", "groupId=" + groupId, ipAddress);
    }

    public List<ContactItemVo> listMembers(Long userId, Long groupId) {
        loadGroup(userId, groupId);
        List<ContactGroupMember> links = contactGroupMemberMapper.selectList(new LambdaQueryWrapper<ContactGroupMember>()
                .eq(ContactGroupMember::getOwnerId, userId)
                .eq(ContactGroupMember::getGroupId, groupId)
                .orderByDesc(ContactGroupMember::getUpdatedAt));
        if (links.isEmpty()) {
            return List.of();
        }

        List<Long> contactIds = links.stream().map(ContactGroupMember::getContactId).toList();
        Map<Long, ContactEntry> contacts = new HashMap<>();
        for (ContactEntry contact : contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .in(ContactEntry::getId, contactIds))) {
            contacts.put(contact.getId(), contact);
        }

        List<ContactItemVo> result = new ArrayList<>();
        for (Long contactId : contactIds) {
            ContactEntry contact = contacts.get(contactId);
            if (contact != null) {
                result.add(toContactVo(contact));
            }
        }
        result.sort(Comparator.comparing(ContactItemVo::displayName));
        return result;
    }

    @Transactional
    public List<ContactItemVo> addMembers(Long userId, Long groupId, List<String> contactIds, String ipAddress) {
        loadGroup(userId, groupId);
        Set<Long> normalizedContactIds = parseAndDistinctContactIds(contactIds);
        ensureContactsOwnedByUser(userId, normalizedContactIds);

        Set<Long> existingContactIds = new LinkedHashSet<>(contactGroupMemberMapper.selectList(new LambdaQueryWrapper<ContactGroupMember>()
                        .eq(ContactGroupMember::getOwnerId, userId)
                        .eq(ContactGroupMember::getGroupId, groupId))
                .stream()
                .map(ContactGroupMember::getContactId)
                .toList());

        LocalDateTime now = LocalDateTime.now();
        int inserted = 0;
        for (Long contactId : normalizedContactIds) {
            if (existingContactIds.contains(contactId)) {
                continue;
            }
            ContactGroupMember member = new ContactGroupMember();
            member.setOwnerId(userId);
            member.setGroupId(groupId);
            member.setContactId(contactId);
            member.setCreatedAt(now);
            member.setUpdatedAt(now);
            member.setDeleted(0);
            contactGroupMemberMapper.insert(member);
            inserted++;
        }

        auditService.record(userId, "CONTACT_GROUP_ADD_MEMBER", "groupId=" + groupId + ",added=" + inserted, ipAddress);
        return listMembers(userId, groupId);
    }

    @Transactional
    public void removeMember(Long userId, Long groupId, Long contactId, String ipAddress) {
        loadGroup(userId, groupId);
        contactGroupMemberMapper.delete(new LambdaQueryWrapper<ContactGroupMember>()
                .eq(ContactGroupMember::getOwnerId, userId)
                .eq(ContactGroupMember::getGroupId, groupId)
                .eq(ContactGroupMember::getContactId, contactId));
        auditService.record(userId, "CONTACT_GROUP_REMOVE_MEMBER", "groupId=" + groupId + ",contactId=" + contactId, ipAddress);
    }

    @Transactional
    public void migrateMemberLinksForMergedContacts(Long userId, Long primaryContactId, List<Long> duplicateContactIds) {
        if (duplicateContactIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ContactGroupMember> duplicateLinks = contactGroupMemberMapper.selectList(new LambdaQueryWrapper<ContactGroupMember>()
                .eq(ContactGroupMember::getOwnerId, userId)
                .in(ContactGroupMember::getContactId, duplicateContactIds));

        for (ContactGroupMember duplicateLink : duplicateLinks) {
            ContactGroupMember existingPrimaryLink = contactGroupMemberMapper.selectOne(new LambdaQueryWrapper<ContactGroupMember>()
                    .eq(ContactGroupMember::getOwnerId, userId)
                    .eq(ContactGroupMember::getGroupId, duplicateLink.getGroupId())
                    .eq(ContactGroupMember::getContactId, primaryContactId));
            if (existingPrimaryLink != null) {
                contactGroupMemberMapper.deleteById(duplicateLink.getId());
                continue;
            }
            duplicateLink.setContactId(primaryContactId);
            duplicateLink.setUpdatedAt(now);
            contactGroupMemberMapper.updateById(duplicateLink);
        }
    }

    private ContactGroup loadGroup(Long userId, Long groupId) {
        ContactGroup group = contactGroupMapper.selectOne(new LambdaQueryWrapper<ContactGroup>()
                .eq(ContactGroup::getOwnerId, userId)
                .eq(ContactGroup::getId, groupId));
        if (group == null) {
            throw new BizException(ErrorCode.CONTACT_GROUP_NOT_FOUND);
        }
        return group;
    }

    private String requireGroupName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Group name is required");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private void ensureGroupNameUnique(Long userId, String name, Long excludeId) {
        ContactGroup existing = contactGroupMapper.selectOne(new LambdaQueryWrapper<ContactGroup>()
                .eq(ContactGroup::getOwnerId, userId)
                .eq(ContactGroup::getName, name));
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw new BizException(ErrorCode.CONTACT_GROUP_ALREADY_EXISTS);
        }
    }

    private Set<Long> parseAndDistinctContactIds(List<String> contactIds) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String raw : contactIds) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(raw.trim()));
            } catch (NumberFormatException ex) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid contact id: " + raw);
            }
        }
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No valid contact ids");
        }
        return ids;
    }

    private void ensureContactsOwnedByUser(Long userId, Set<Long> contactIds) {
        List<ContactEntry> contacts = contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .in(ContactEntry::getId, contactIds));
        if (contacts.size() != contactIds.size()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Some contacts are invalid");
        }
    }

    private int countMembers(Long userId, Long groupId) {
        return Math.toIntExact(contactGroupMemberMapper.selectCount(new LambdaQueryWrapper<ContactGroupMember>()
                .eq(ContactGroupMember::getOwnerId, userId)
                .eq(ContactGroupMember::getGroupId, groupId)));
    }

    private Map<Long, Integer> buildMemberCountMap(Long userId, List<ContactGroup> groups) {
        Map<Long, Integer> memberCountMap = new HashMap<>();
        if (groups.isEmpty()) {
            return memberCountMap;
        }
        List<Long> groupIds = groups.stream().map(ContactGroup::getId).toList();
        List<ContactGroupMember> members = contactGroupMemberMapper.selectList(new LambdaQueryWrapper<ContactGroupMember>()
                .eq(ContactGroupMember::getOwnerId, userId)
                .in(ContactGroupMember::getGroupId, groupIds));
        for (ContactGroupMember member : members) {
            memberCountMap.merge(member.getGroupId(), 1, Integer::sum);
        }
        return memberCountMap;
    }

    private ContactGroupItemVo toGroupVo(ContactGroup group, int memberCount) {
        return new ContactGroupItemVo(
                String.valueOf(group.getId()),
                group.getName(),
                group.getDescription(),
                memberCount,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    private ContactItemVo toContactVo(ContactEntry entry) {
        return new ContactItemVo(
                String.valueOf(entry.getId()),
                entry.getDisplayName(),
                entry.getEmail(),
                entry.getNote(),
                entry.getIsFavorite() != null && entry.getIsFavorite() == 1,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
