package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.UpdatePassAliasRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.dto.V21PassSecureLinkRequest;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.vo.PassItemDetailVo;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassMailAliasVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import com.mmmail.server.model.vo.PassSecureLinkDashboardVo;
import com.mmmail.server.model.vo.PassSecureLinkVo;
import com.mmmail.server.model.vo.V21PassVaultVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class V21PassRuntimeBridgeService {

    private static final String PERSONAL_VAULT_ID = "personal";
    private static final String PERSONAL_VAULT_NAME = "Personal";
    private static final String INVALID_PASS_ID = "Pass id is invalid";

    private final PassService passService;
    private final PassBusinessService passBusinessService;
    private final PassAliasService passAliasService;
    private final PassMonitorService passMonitorService;
    private final PassVaultItemMapper passVaultItemMapper;

    public V21PassRuntimeBridgeService(
            PassService passService,
            PassBusinessService passBusinessService,
            PassAliasService passAliasService,
            PassMonitorService passMonitorService,
            PassVaultItemMapper passVaultItemMapper
    ) {
        this.passService = passService;
        this.passBusinessService = passBusinessService;
        this.passAliasService = passAliasService;
        this.passMonitorService = passMonitorService;
        this.passVaultItemMapper = passVaultItemMapper;
    }

    public List<V21PassVaultVo> listVaults(Long userId, String ownerEmail) {
        Long itemCount = passVaultItemMapper.selectCount(personalItemQuery(userId));
        PassVaultItem latestItem = passVaultItemMapper.selectOne(personalItemQuery(userId)
                .orderByDesc(PassVaultItem::getUpdatedAt)
                .last("limit 1"));
        LocalDateTime updatedAt = latestItem == null ? null : latestItem.getUpdatedAt();
        return List.of(new V21PassVaultVo(
                PERSONAL_VAULT_ID,
                PERSONAL_VAULT_NAME,
                PassBusinessConstants.SCOPE_PERSONAL,
                ownerEmail,
                Math.toIntExact(itemCount),
                updatedAt
        ));
    }

    public List<PassItemSummaryVo> listItems(
            Long userId,
            String keyword,
            Boolean favoriteOnly,
            Integer limit,
            String itemType
    ) {
        return passService.list(userId, keyword, favoriteOnly, limit, itemType);
    }

    public PassItemSummaryVo createItem(Long userId, CreatePassItemRequest request, String ipAddress) {
        return toSummary(passService.create(
                userId,
                request.title(),
                request.itemType(),
                request.website(),
                request.username(),
                request.secretCiphertext(),
                request.note(),
                ipAddress
        ));
    }

    public PassItemSummaryVo updateItem(
            Long userId,
            String itemId,
            UpdatePassItemRequest request,
            String ipAddress
    ) {
        return toSummary(passService.update(
                userId,
                parseId(itemId),
                request.title(),
                request.itemType(),
                request.website(),
                request.username(),
                request.secretCiphertext(),
                request.note(),
                ipAddress
        ));
    }

    public PassMonitorOverviewVo readMonitor(Long userId, String ipAddress) {
        return passMonitorService.getPersonalMonitor(userId, ipAddress);
    }

    public List<PassMailAliasVo> listAliases(Long userId, String ipAddress) {
        return passAliasService.listAliases(userId, ipAddress);
    }

    public PassMailAliasVo updateAlias(
            Long userId,
            String aliasId,
            UpdatePassAliasRequest request,
            String ipAddress
    ) {
        return passAliasService.updateAlias(userId, parseId(aliasId), request, ipAddress);
    }

    public List<PassSecureLinkDashboardVo> listSecureLinks(
            Long userId,
            Long orgId,
            String publicBaseUrl,
            String ipAddress
    ) {
        return passBusinessService.listOrgSecureLinks(userId, orgId, publicBaseUrl, ipAddress);
    }

    public PassSecureLinkVo createSecureLink(
            Long userId,
            V21PassSecureLinkRequest request,
            String publicBaseUrl,
            String ipAddress
    ) {
        return passBusinessService.createSecureLink(
                userId,
                request.orgId(),
                request.itemId(),
                request.toCreateRequest(),
                publicBaseUrl,
                ipAddress
        );
    }

    public void revokeSecureLink(
            Long userId,
            Long orgId,
            String linkId,
            String publicBaseUrl,
            String ipAddress
    ) {
        passBusinessService.revokeSecureLink(userId, orgId, parseId(linkId), publicBaseUrl, ipAddress);
    }

    private PassItemSummaryVo toSummary(PassItemDetailVo detail) {
        return new PassItemSummaryVo(
                detail.id(),
                detail.title(),
                detail.website(),
                detail.username(),
                detail.favorite(),
                detail.updatedAt(),
                detail.scopeType(),
                detail.itemType(),
                detail.sharedVaultId(),
                detail.secureLinkCount()
        );
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, INVALID_PASS_ID);
        }
    }

    private LambdaQueryWrapper<PassVaultItem> personalItemQuery(Long userId) {
        return new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOwnerId, userId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_PERSONAL);
    }
}
