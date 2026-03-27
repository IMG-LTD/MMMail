package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.PassAliasContactMapper;
import com.mmmail.server.mapper.PassAliasMailboxRouteMapper;
import com.mmmail.server.mapper.PassMailAliasMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreatePassAliasRequest;
import com.mmmail.server.model.dto.UpdatePassAliasRequest;
import com.mmmail.server.model.entity.PassAliasContact;
import com.mmmail.server.model.entity.PassAliasMailboxRoute;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.PassMailbox;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.MailSenderIdentityVo;
import com.mmmail.server.model.vo.PassMailAliasVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PassAliasService {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String SOURCE_PASS_ALIAS = "PASS_ALIAS";

    private static final int DEFAULT_FALSE = 0;
    private static final String ALIAS_DOMAIN = "passmail.mmmail.local";
    private static final int RANDOM_SUFFIX_LENGTH = 6;
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9._+-]{0,31}[a-z0-9])?$");
    private static final String RANDOM_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PassMailAliasMapper passMailAliasMapper;
    private final PassAliasMailboxRouteMapper passAliasMailboxRouteMapper;
    private final PassAliasContactMapper passAliasContactMapper;
    private final UserAccountMapper userAccountMapper;
    private final PassMailboxService passMailboxService;
    private final AuditService auditService;

    public PassAliasService(
            PassMailAliasMapper passMailAliasMapper,
            PassAliasMailboxRouteMapper passAliasMailboxRouteMapper,
            PassAliasContactMapper passAliasContactMapper,
            UserAccountMapper userAccountMapper,
            PassMailboxService passMailboxService,
            AuditService auditService
    ) {
        this.passMailAliasMapper = passMailAliasMapper;
        this.passAliasMailboxRouteMapper = passAliasMailboxRouteMapper;
        this.passAliasContactMapper = passAliasContactMapper;
        this.userAccountMapper = userAccountMapper;
        this.passMailboxService = passMailboxService;
        this.auditService = auditService;
    }

    public List<PassMailAliasVo> listAliases(Long userId, String ipAddress) {
        passMailboxService.ensurePrimaryMailbox(userId);
        List<PassMailAlias> aliases = passMailAliasMapper.selectList(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .orderByDesc(PassMailAlias::getUpdatedAt));
        Map<Long, List<String>> routeEmails = loadRouteEmailMap(aliases);
        List<PassMailAliasVo> result = aliases.stream()
                .map(alias -> toVo(alias, routeEmails.get(alias.getId())))
                .toList();
        auditService.record(userId, "PASS_ALIAS_LIST", "count=" + result.size(), ipAddress);
        return result;
    }

    @Transactional
    public PassMailAliasVo createAlias(Long userId, CreatePassAliasRequest request, String ipAddress) {
        requireUser(userId);
        List<PassMailbox> routeMailboxes = passMailboxService.resolveAliasForwardTargets(
                userId,
                request.forwardToEmail(),
                request.forwardToEmails()
        );
        String aliasEmail = buildUniqueAliasEmail(normalizePrefix(request.prefix()));
        LocalDateTime now = LocalDateTime.now();
        PassMailAlias alias = buildAlias(userId, aliasEmail, request.title(), request.note(), routeMailboxes.get(0).getMailboxEmail(), now);
        passMailAliasMapper.insert(alias);
        replaceRoutes(alias.getId(), userId, routeMailboxes, now);
        auditService.record(
                userId,
                "PASS_ALIAS_CREATE",
                "alias=" + aliasEmail + ",routeCount=" + routeMailboxes.size(),
                ipAddress
        );
        return toVo(alias, routeMailboxes.stream().map(PassMailbox::getMailboxEmail).toList());
    }

    @Transactional
    public PassMailAliasVo updateAlias(Long userId, Long aliasId, UpdatePassAliasRequest request, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        List<PassMailbox> routeMailboxes = passMailboxService.resolveAliasForwardTargets(
                userId,
                request.forwardToEmail(),
                request.forwardToEmails()
        );
        alias.setTitle(requireTitle(request.title()));
        alias.setNote(normalizeNote(request.note()));
        alias.setForwardToEmail(routeMailboxes.get(0).getMailboxEmail());
        alias.setUpdatedAt(LocalDateTime.now());
        passMailAliasMapper.updateById(alias);
        replaceRoutes(alias.getId(), userId, routeMailboxes, alias.getUpdatedAt());
        auditService.record(
                userId,
                "PASS_ALIAS_UPDATE",
                "aliasId=" + aliasId + ",routeCount=" + routeMailboxes.size(),
                ipAddress
        );
        return toVo(alias, routeMailboxes.stream().map(PassMailbox::getMailboxEmail).toList());
    }

    @Transactional
    public PassMailAliasVo enableAlias(Long userId, Long aliasId, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        alias.setStatus(STATUS_ENABLED);
        alias.setUpdatedAt(LocalDateTime.now());
        passMailAliasMapper.updateById(alias);
        auditService.record(userId, "PASS_ALIAS_ENABLE", "aliasId=" + aliasId + ",email=" + alias.getAliasEmail(), ipAddress);
        return toVo(alias, resolveRouteEmails(alias));
    }

    @Transactional
    public PassMailAliasVo disableAlias(Long userId, Long aliasId, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        alias.setStatus(STATUS_DISABLED);
        alias.setUpdatedAt(LocalDateTime.now());
        passMailAliasMapper.updateById(alias);
        auditService.record(userId, "PASS_ALIAS_DISABLE", "aliasId=" + aliasId + ",email=" + alias.getAliasEmail(), ipAddress);
        return toVo(alias, resolveRouteEmails(alias));
    }

    @Transactional
    public void deleteAlias(Long userId, Long aliasId, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        passAliasContactMapper.delete(new LambdaQueryWrapper<PassAliasContact>()
                .eq(PassAliasContact::getOwnerId, userId)
                .eq(PassAliasContact::getAliasId, aliasId));
        passAliasMailboxRouteMapper.delete(new LambdaQueryWrapper<PassAliasMailboxRoute>()
                .eq(PassAliasMailboxRoute::getOwnerId, userId)
                .eq(PassAliasMailboxRoute::getAliasId, aliasId));
        passMailAliasMapper.deleteById(alias.getId());
        auditService.record(userId, "PASS_ALIAS_DELETE", "aliasId=" + aliasId + ",email=" + alias.getAliasEmail(), ipAddress);
    }

    public PassMailAlias loadEnabledAliasByEmail(String emailAddress) {
        String normalized = normalizeEmail(emailAddress);
        return passMailAliasMapper.selectOne(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getAliasEmail, normalized)
                .eq(PassMailAlias::getStatus, STATUS_ENABLED)
                .last("limit 1"));
    }

    public List<String> resolveRouteEmails(PassMailAlias alias) {
        List<PassAliasMailboxRoute> routes = passAliasMailboxRouteMapper.selectList(new LambdaQueryWrapper<PassAliasMailboxRoute>()
                .eq(PassAliasMailboxRoute::getAliasId, alias.getId())
                .orderByAsc(PassAliasMailboxRoute::getCreatedAt));
        if (!routes.isEmpty()) {
            return routes.stream().map(PassAliasMailboxRoute::getMailboxEmail).toList();
        }
        if (StringUtils.hasText(alias.getForwardToEmail())) {
            return List.of(alias.getForwardToEmail());
        }
        return List.of();
    }

    public String resolveAuthorizedAliasEmailOrNull(Long userId, String emailAddress) {
        String normalized = normalizeEmail(emailAddress);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        PassMailAlias alias = passMailAliasMapper.selectOne(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .eq(PassMailAlias::getAliasEmail, normalized)
                .eq(PassMailAlias::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        return alias == null ? null : alias.getAliasEmail();
    }

    public List<MailSenderIdentityVo> listAliasSenderIdentities(Long userId) {
        requireUser(userId);
        List<PassMailAlias> aliases = passMailAliasMapper.selectList(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .eq(PassMailAlias::getStatus, STATUS_ENABLED)
                .orderByDesc(PassMailAlias::getUpdatedAt));
        List<MailSenderIdentityVo> result = new ArrayList<>();
        for (PassMailAlias alias : aliases) {
            result.add(new MailSenderIdentityVo(
                    String.valueOf(alias.getId()),
                    null,
                    null,
                    null,
                    alias.getAliasEmail(),
                    alias.getTitle(),
                    SOURCE_PASS_ALIAS,
                    alias.getStatus(),
                    false
            ));
        }
        return result;
    }

    private Map<Long, List<String>> loadRouteEmailMap(List<PassMailAlias> aliases) {
        if (aliases.isEmpty()) {
            return Map.of();
        }
        List<Long> aliasIds = aliases.stream().map(PassMailAlias::getId).toList();
        List<PassAliasMailboxRoute> routes = passAliasMailboxRouteMapper.selectList(new LambdaQueryWrapper<PassAliasMailboxRoute>()
                .in(PassAliasMailboxRoute::getAliasId, aliasIds)
                .orderByAsc(PassAliasMailboxRoute::getCreatedAt));
        Map<Long, List<String>> routeMap = new LinkedHashMap<>();
        for (PassAliasMailboxRoute route : routes) {
            routeMap.computeIfAbsent(route.getAliasId(), ignored -> new ArrayList<>()).add(route.getMailboxEmail());
        }
        for (PassMailAlias alias : aliases) {
            routeMap.computeIfAbsent(alias.getId(), ignored -> fallbackRoute(alias));
        }
        return routeMap;
    }

    private List<String> fallbackRoute(PassMailAlias alias) {
        if (!StringUtils.hasText(alias.getForwardToEmail())) {
            return List.of();
        }
        return List.of(alias.getForwardToEmail());
    }

    private void replaceRoutes(Long aliasId, Long userId, List<PassMailbox> routeMailboxes, LocalDateTime now) {
        passAliasMailboxRouteMapper.delete(new LambdaQueryWrapper<PassAliasMailboxRoute>()
                .eq(PassAliasMailboxRoute::getAliasId, aliasId));
        for (PassMailbox mailbox : routeMailboxes) {
            PassAliasMailboxRoute route = new PassAliasMailboxRoute();
            route.setAliasId(aliasId);
            route.setOwnerId(userId);
            route.setMailboxId(mailbox.getId());
            route.setMailboxEmail(mailbox.getMailboxEmail());
            route.setCreatedAt(now);
            route.setUpdatedAt(now);
            route.setDeleted(DEFAULT_FALSE);
            passAliasMailboxRouteMapper.insert(route);
        }
    }

    private PassMailAlias buildAlias(Long userId, String aliasEmail, String title, String note, String forwardToEmail, LocalDateTime now) {
        PassMailAlias alias = new PassMailAlias();
        alias.setOwnerId(userId);
        alias.setAliasEmail(aliasEmail);
        alias.setTitle(requireTitle(title));
        alias.setNote(normalizeNote(note));
        alias.setForwardToEmail(forwardToEmail);
        alias.setStatus(STATUS_ENABLED);
        alias.setCreatedAt(now);
        alias.setUpdatedAt(now);
        alias.setDeleted(DEFAULT_FALSE);
        return alias;
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private PassMailAlias loadOwnedAlias(Long userId, Long aliasId) {
        PassMailAlias alias = passMailAliasMapper.selectOne(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getId, aliasId)
                .eq(PassMailAlias::getOwnerId, userId)
                .last("limit 1"));
        if (alias == null) {
            throw new BizException(ErrorCode.PASS_ALIAS_NOT_FOUND, "Pass alias not found");
        }
        return alias;
    }

    private String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Alias title is required");
        }
        return title.trim();
    }

    private String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    private String buildUniqueAliasEmail(String normalizedPrefix) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String base = StringUtils.hasText(normalizedPrefix) ? normalizedPrefix : randomSegment(10);
            String email = (base + "-" + randomSegment(RANDOM_SUFFIX_LENGTH) + "@" + ALIAS_DOMAIN).toLowerCase(Locale.ROOT);
            if (passMailAliasMapper.selectCount(new LambdaQueryWrapper<PassMailAlias>()
                    .eq(PassMailAlias::getAliasEmail, email)) == 0) {
                return email;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Unable to allocate alias email");
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return null;
        }
        String normalized = prefix.trim().toLowerCase(Locale.ROOT);
        if (!PREFIX_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Alias prefix is invalid");
        }
        return normalized;
    }

    private String normalizeEmail(String emailAddress) {
        return StringUtils.hasText(emailAddress) ? emailAddress.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String randomSegment(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(RANDOM_ALPHABET.charAt(RANDOM.nextInt(RANDOM_ALPHABET.length())));
        }
        return builder.toString();
    }

    private PassMailAliasVo toVo(PassMailAlias alias, List<String> forwardToEmails) {
        List<String> routes = forwardToEmails == null || forwardToEmails.isEmpty()
                ? fallbackRoute(alias)
                : List.copyOf(forwardToEmails);
        String primaryForward = routes.isEmpty() ? alias.getForwardToEmail() : routes.get(0);
        return new PassMailAliasVo(
                String.valueOf(alias.getId()),
                alias.getAliasEmail(),
                alias.getTitle(),
                alias.getNote(),
                primaryForward,
                routes,
                alias.getStatus(),
                alias.getCreatedAt(),
                alias.getUpdatedAt()
        );
    }
}
