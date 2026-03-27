package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgPolicyMapper;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.model.dto.GeneratePasswordRequest;
import com.mmmail.server.model.dto.UpsertPassItemTwoFactorRequest;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgPolicy;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import com.mmmail.server.model.vo.GeneratedPasswordVo;
import com.mmmail.server.model.vo.PassItemDetailVo;
import com.mmmail.server.model.vo.PassItemSummaryVo;
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
import java.util.Set;

@Service
public class PassService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_PASSWORD_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;
    private static final int MIN_MEMORABLE_SEGMENTS = 2;
    private static final int MAX_MEMORABLE_ATTEMPTS = 48;

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}:;,.?";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PassVaultItemMapper passVaultItemMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgPolicyMapper orgPolicyMapper;
    private final AuditService auditService;
    private final PassItemTwoFactorSupport passItemTwoFactorSupport;
    private final TotpCodeService totpCodeService;

    public PassService(
            PassVaultItemMapper passVaultItemMapper,
            OrgMemberMapper orgMemberMapper,
            OrgPolicyMapper orgPolicyMapper,
            AuditService auditService,
            PassItemTwoFactorSupport passItemTwoFactorSupport,
            TotpCodeService totpCodeService
    ) {
        this.passVaultItemMapper = passVaultItemMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.orgPolicyMapper = orgPolicyMapper;
        this.auditService = auditService;
        this.passItemTwoFactorSupport = passItemTwoFactorSupport;
        this.totpCodeService = totpCodeService;
    }

    public List<PassItemSummaryVo> list(Long userId, String keyword, Boolean favoriteOnly, Integer limit) {
        return list(userId, keyword, favoriteOnly, limit, null);
    }

    public List<PassItemSummaryVo> list(Long userId, String keyword, Boolean favoriteOnly, Integer limit, String itemType) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedItemType = normalizeItemType(itemType);
        LambdaQueryWrapper<PassVaultItem> query = new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOwnerId, userId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_PERSONAL)
                .orderByDesc(PassVaultItem::getUpdatedAt)
                .last("limit " + safeLimit);
        if (Boolean.TRUE.equals(favoriteOnly)) {
            query.eq(PassVaultItem::getFavorite, 1);
        }
        if (normalizedItemType != null) {
            query.eq(PassVaultItem::getItemType, normalizedItemType);
        }
        if (StringUtils.hasText(normalizedKeyword)) {
            query.and(wrapper -> wrapper
                    .like(PassVaultItem::getTitle, normalizedKeyword)
                    .or()
                    .like(PassVaultItem::getWebsite, normalizedKeyword)
                    .or()
                    .like(PassVaultItem::getUsername, normalizedKeyword));
        }
        return passVaultItemMapper.selectList(query).stream().map(this::toSummaryVo).toList();
    }

    @Transactional
    public PassItemDetailVo create(
            Long userId,
            String title,
            String website,
            String username,
            String secretCiphertext,
            String note,
            String ipAddress
    ) {
        return create(userId, title, null, website, username, secretCiphertext, note, ipAddress);
    }

    @Transactional
    public PassItemDetailVo create(
            Long userId,
            String title,
            String itemType,
            String website,
            String username,
            String secretCiphertext,
            String note,
            String ipAddress
    ) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedType = normalizeItemType(itemType);
        PassVaultItem item = new PassVaultItem();
        item.setOwnerId(userId);
        item.setOrgId(null);
        item.setSharedVaultId(null);
        item.setScopeType(PassBusinessConstants.SCOPE_PERSONAL);
        item.setItemType(normalizedType == null ? PassBusinessConstants.ITEM_TYPE_LOGIN : normalizedType);
        item.setTitle(requireTitle(title));
        item.setWebsite(normalizeWebsite(website));
        item.setUsername(normalizeUsername(username));
        item.setSecretCiphertext(normalizeSecret(item.getItemType(), secretCiphertext));
        clearTwoFactorIfUnsupported(item);
        item.setNote(normalizeNote(note));
        item.setFavorite(0);
        item.setMonitorExcluded(0);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        passVaultItemMapper.insert(item);
        auditService.record(userId, "PASS_ITEM_CREATE", "itemId=" + item.getId() + ",itemType=" + item.getItemType(), ipAddress);
        return toDetailVo(item);
    }

    public PassItemDetailVo get(Long userId, Long itemId) {
        return toDetailVo(loadItem(userId, itemId));
    }

    @Transactional
    public PassItemDetailVo update(
            Long userId,
            Long itemId,
            String title,
            String itemType,
            String website,
            String username,
            String secretCiphertext,
            String note,
            String ipAddress
    ) {
        PassVaultItem item = loadItem(userId, itemId);
        String normalizedType = normalizeItemType(itemType);
        if (normalizedType != null) {
            item.setItemType(normalizedType);
        }
        item.setTitle(requireTitle(title));
        item.setWebsite(normalizeWebsite(website));
        item.setUsername(normalizeUsername(username));
        item.setSecretCiphertext(normalizeSecret(item.getItemType(), secretCiphertext));
        clearTwoFactorIfUnsupported(item);
        item.setNote(normalizeNote(note));
        item.setUpdatedAt(LocalDateTime.now());
        passVaultItemMapper.updateById(item);
        auditService.record(userId, "PASS_ITEM_UPDATE", "itemId=" + itemId + ",itemType=" + item.getItemType(), ipAddress);
        return toDetailVo(item);
    }

    @Transactional
    public void delete(Long userId, Long itemId, String ipAddress) {
        PassVaultItem item = loadItem(userId, itemId);
        passVaultItemMapper.deleteById(item.getId());
        auditService.record(userId, "PASS_ITEM_DELETE", "itemId=" + itemId, ipAddress);
    }

    @Transactional
    public PassItemDetailVo favorite(Long userId, Long itemId, boolean favorite, String ipAddress) {
        PassVaultItem item = loadItem(userId, itemId);
        item.setFavorite(favorite ? 1 : 0);
        item.setUpdatedAt(LocalDateTime.now());
        passVaultItemMapper.updateById(item);
        auditService.record(userId, "PASS_ITEM_FAVORITE", "itemId=" + itemId + ",favorite=" + favorite, ipAddress);
        return toDetailVo(item);
    }

    @Transactional
    public PassItemDetailVo upsertTwoFactor(
            Long userId,
            Long itemId,
            UpsertPassItemTwoFactorRequest request,
            String ipAddress
    ) {
        PassVaultItem item = loadItem(userId, itemId);
        ensureTwoFactorSupported(item);
        passItemTwoFactorSupport.apply(
                item,
                passItemTwoFactorSupport.normalize(
                        request.issuer(),
                        request.accountName(),
                        request.secretCiphertext(),
                        request.algorithm(),
                        request.digits(),
                        request.periodSeconds()
                ),
                LocalDateTime.now()
        );
        passVaultItemMapper.updateById(item);
        auditService.record(userId, "PASS_ITEM_2FA_SAVE", "itemId=" + itemId, ipAddress);
        return toDetailVo(item);
    }

    @Transactional
    public PassItemDetailVo deleteTwoFactor(Long userId, Long itemId, String ipAddress) {
        PassVaultItem item = loadItem(userId, itemId);
        ensureTwoFactorSupported(item);
        passItemTwoFactorSupport.clear(item, LocalDateTime.now());
        passVaultItemMapper.updateById(item);
        auditService.record(userId, "PASS_ITEM_2FA_DELETE", "itemId=" + itemId, ipAddress);
        return toDetailVo(item);
    }

    public AuthenticatorCodeVo generateTwoFactorCode(Long userId, Long itemId, String ipAddress) {
        PassVaultItem item = loadItem(userId, itemId);
        ensureHasTwoFactor(item);
        AuthenticatorCodeVo code = totpCodeService.generateCode(
                item.getTwoFactorSecretCiphertext(),
                passItemTwoFactorSupport.toVo(item).algorithm(),
                passItemTwoFactorSupport.toVo(item).digits(),
                passItemTwoFactorSupport.toVo(item).periodSeconds()
        );
        auditService.record(
                userId,
                "PASS_ITEM_2FA_CODE_GENERATE",
                "itemId=" + itemId + ",digits=" + code.digits() + ",periodSeconds=" + code.periodSeconds(),
                ipAddress
        );
        return code;
    }

    public GeneratedPasswordVo generatePassword(
            Long userId,
            GeneratePasswordRequest request,
            String ipAddress
    ) {
        GeneratePasswordRequest safeRequest = request == null
                ? new GeneratePasswordRequest(null, null, null, null, null, null, null)
                : request;
        PasswordGeneratorPolicy policy = loadGeneratorPolicy(userId, safeRequest.orgId());
        int safeLength = resolvePasswordLength(safeRequest.length(), policy);
        boolean memorable = boolDefault(safeRequest.memorable(), false);
        String generated = memorable
                ? buildMemorablePassword(safeLength, policy.allowMemorablePasswords())
                : buildRandomPassword(safeLength, safeRequest, policy);
        auditService.record(
                userId,
                "PASS_PASSWORD_GENERATE",
                "orgId=" + safeRequest.orgId()
                        + ",requestedLength=" + safeLength
                        + ",generatedLength=" + generated.length()
                        + ",memorable=" + memorable,
                ipAddress,
                safeRequest.orgId()
        );
        return new GeneratedPasswordVo(generated);
    }

    public GeneratedPasswordVo generatePassword(
            Long userId,
            Integer length,
            Boolean includeLowercase,
            Boolean includeUppercase,
            Boolean includeDigits,
            Boolean includeSymbols,
            String ipAddress
    ) {
        return generatePassword(
                userId,
                new GeneratePasswordRequest(null, length, includeLowercase, includeUppercase, includeDigits, includeSymbols, false),
                ipAddress
        );
    }

    private String buildRandomPassword(int length, GeneratePasswordRequest request, PasswordGeneratorPolicy policy) {
        List<String> pools = buildPools(request, policy);
        return new String(buildPassword(length, pools));
    }

    private List<String> buildPools(GeneratePasswordRequest request, PasswordGeneratorPolicy policy) {
        boolean useLower = boolDefault(request.includeLowercase(), true);
        boolean useUpper = policy.requireUppercase() || boolDefault(request.includeUppercase(), true);
        boolean useDigits = policy.requireDigits() || boolDefault(request.includeDigits(), true);
        boolean useSymbols = policy.requireSymbols() || boolDefault(request.includeSymbols(), true);
        List<String> pools = new ArrayList<>();
        if (useLower) {
            pools.add(LOWER);
        }
        if (useUpper) {
            pools.add(UPPER);
        }
        if (useDigits) {
            pools.add(DIGITS);
        }
        if (useSymbols) {
            pools.add(SYMBOLS);
        }
        if (pools.isEmpty()) {
            pools.add(LOWER);
            pools.add(UPPER);
            pools.add(DIGITS);
        }
        return pools;
    }

    private char[] buildPassword(int length, List<String> pools) {
        StringBuilder all = new StringBuilder();
        for (String pool : pools) {
            all.append(pool);
        }
        List<Character> characters = new ArrayList<>();
        for (String pool : pools) {
            characters.add(randomChar(pool));
        }
        while (characters.size() < length) {
            characters.add(randomChar(all.toString()));
        }
        for (int index = characters.size() - 1; index > 0; index--) {
            int swapIndex = RANDOM.nextInt(index + 1);
            Character temp = characters.get(index);
            characters.set(index, characters.get(swapIndex));
            characters.set(swapIndex, temp);
        }
        char[] result = new char[length];
        for (int index = 0; index < length; index++) {
            result[index] = characters.get(index);
        }
        return result;
    }

    private char randomChar(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }

    private String buildMemorablePassword(int maxLength, boolean allowMemorablePasswords) {
        if (!allowMemorablePasswords) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Pass policy does not allow memorable passwords");
        }
        for (int attempt = 0; attempt < MAX_MEMORABLE_ATTEMPTS; attempt++) {
            String candidate = createMemorableCandidate(maxLength);
            if (candidate.length() <= maxLength) {
                return candidate;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to generate memorable password");
    }

    private String createMemorableCandidate(int maxLength) {
        List<String> parts = new ArrayList<>();
        int projectedLength = 0;
        while (parts.size() < 5) {
            String nextWord = PassMemorableWords.WORDS.get(RANDOM.nextInt(PassMemorableWords.WORDS.size()));
            int nextLength = projectedLength + nextWord.length() + (parts.isEmpty() ? 0 : 1);
            if (nextLength > maxLength) {
                break;
            }
            parts.add(nextWord);
            projectedLength = nextLength;
            if (parts.size() >= MIN_MEMORABLE_SEGMENTS && projectedLength >= maxLength - 2) {
                break;
            }
        }
        if (parts.size() < MIN_MEMORABLE_SEGMENTS) {
            return "%s-%s".formatted(
                    PassMemorableWords.WORDS.get(RANDOM.nextInt(PassMemorableWords.WORDS.size())),
                    PassMemorableWords.WORDS.get(RANDOM.nextInt(PassMemorableWords.WORDS.size()))
            );
        }
        return String.join("-", parts);
    }

    private PasswordGeneratorPolicy loadGeneratorPolicy(Long userId, Long orgId) {
        if (orgId == null) {
            return PasswordGeneratorPolicy.personalDefault();
        }
        requireActiveOrgMembership(userId, orgId);
        Map<String, String> valueMap = new LinkedHashMap<>();
        List<OrgPolicy> policies = orgPolicyMapper.selectList(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId)
                .in(OrgPolicy::getPolicyKey, generatorPolicyKeys())
                .orderByDesc(OrgPolicy::getUpdatedAt));
        for (OrgPolicy policy : policies) {
            valueMap.putIfAbsent(policy.getPolicyKey(), policy.getPolicyValue());
        }
        int minimumLength = parseInt(valueMap.get(PassBusinessConstants.POLICY_MINIMUM_PASSWORD_LENGTH), PassBusinessConstants.DEFAULT_MINIMUM_PASSWORD_LENGTH);
        int maximumLength = parseInt(valueMap.get(PassBusinessConstants.POLICY_MAXIMUM_PASSWORD_LENGTH), PassBusinessConstants.DEFAULT_MAXIMUM_PASSWORD_LENGTH);
        if (maximumLength < minimumLength) {
            maximumLength = minimumLength;
        }
        return new PasswordGeneratorPolicy(
                minimumLength,
                maximumLength,
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_REQUIRE_UPPERCASE), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_REQUIRE_DIGITS), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_REQUIRE_SYMBOLS), true),
                parseBoolean(
                        valueMap.get(PassBusinessConstants.POLICY_ALLOW_MEMORABLE_PASSWORDS),
                        PassBusinessConstants.DEFAULT_ALLOW_MEMORABLE_PASSWORDS
                )
        );
    }

    private List<String> generatorPolicyKeys() {
        return List.of(
                PassBusinessConstants.POLICY_MINIMUM_PASSWORD_LENGTH,
                PassBusinessConstants.POLICY_MAXIMUM_PASSWORD_LENGTH,
                PassBusinessConstants.POLICY_REQUIRE_UPPERCASE,
                PassBusinessConstants.POLICY_REQUIRE_DIGITS,
                PassBusinessConstants.POLICY_REQUIRE_SYMBOLS,
                PassBusinessConstants.POLICY_ALLOW_MEMORABLE_PASSWORDS
        );
    }

    private void requireActiveOrgMembership(Long userId, Long orgId) {
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId));
        if (member == null || !PassBusinessConstants.ORG_STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No access to this organization");
        }
    }

    private PassVaultItem loadItem(Long userId, Long itemId) {
        PassVaultItem item = passVaultItemMapper.selectOne(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getId, itemId)
                .eq(PassVaultItem::getOwnerId, userId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_PERSONAL));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item is not found");
        }
        return item;
    }

    private String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item title is required");
        }
        return title.trim();
    }

    private String normalizeSecret(String itemType, String secretCiphertext) {
        if (requiresSecret(itemType) && !StringUtils.hasText(secretCiphertext)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item password is required");
        }
        return StringUtils.hasText(secretCiphertext) ? secretCiphertext : null;
    }

    private boolean requiresSecret(String itemType) {
        return Set.of(
                PassBusinessConstants.ITEM_TYPE_LOGIN,
                PassBusinessConstants.ITEM_TYPE_PASSWORD,
                PassBusinessConstants.ITEM_TYPE_CARD,
                PassBusinessConstants.ITEM_TYPE_PASSKEY
        ).contains(itemType);
    }

    private String normalizeItemType(String itemType) {
        if (!StringUtils.hasText(itemType)) {
            return null;
        }
        String normalized = itemType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(
                PassBusinessConstants.ITEM_TYPE_LOGIN,
                PassBusinessConstants.ITEM_TYPE_PASSWORD,
                PassBusinessConstants.ITEM_TYPE_NOTE,
                PassBusinessConstants.ITEM_TYPE_CARD,
                PassBusinessConstants.ITEM_TYPE_ALIAS,
                PassBusinessConstants.ITEM_TYPE_PASSKEY
        ).contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported pass item type");
        }
        return normalized;
    }

    private String normalizeWebsite(String website) {
        return StringUtils.hasText(website) ? website.trim() : null;
    }

    private String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : null;
    }

    private String normalizeNote(String note) {
        return note == null ? "" : note;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private void clearTwoFactorIfUnsupported(PassVaultItem item) {
        if (!passItemTwoFactorSupport.supports(item)) {
            passItemTwoFactorSupport.clear(item, item.getUpdatedAt() == null ? LocalDateTime.now() : item.getUpdatedAt());
        }
    }

    private void ensureTwoFactorSupported(PassVaultItem item) {
        if (!passItemTwoFactorSupport.supports(item)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item type does not support built-in 2FA");
        }
    }

    private void ensureHasTwoFactor(PassVaultItem item) {
        ensureTwoFactorSupported(item);
        if (!passItemTwoFactorSupport.hasTwoFactor(item)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item 2FA is not configured");
        }
    }

    private int resolvePasswordLength(Integer length, PasswordGeneratorPolicy policy) {
        int requested = length == null ? DEFAULT_PASSWORD_LENGTH : length;
        if (requested < MIN_PASSWORD_LENGTH || requested > MAX_PASSWORD_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Password length must be between 8 and 64");
        }
        if (requested < policy.minimumLength() || requested > policy.maximumLength()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Password length must be between %d and %d for this organization".formatted(
                            policy.minimumLength(),
                            policy.maximumLength()
                    )
            );
        }
        return requested;
    }

    private boolean boolDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int parseInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private PassItemSummaryVo toSummaryVo(PassVaultItem item) {
        return new PassItemSummaryVo(
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getFavorite() != null && item.getFavorite() == 1,
                item.getUpdatedAt(),
                item.getScopeType(),
                item.getItemType(),
                null,
                0
        );
    }

    private PassItemDetailVo toDetailVo(PassVaultItem item) {
        return new PassItemDetailVo(
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getSecretCiphertext(),
                item.getNote(),
                item.getFavorite() != null && item.getFavorite() == 1,
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getScopeType(),
                item.getItemType(),
                null,
                null,
                null,
                0,
                passItemTwoFactorSupport.toVo(item)
        );
    }

    private record PasswordGeneratorPolicy(
            int minimumLength,
            int maximumLength,
            boolean requireUppercase,
            boolean requireDigits,
            boolean requireSymbols,
            boolean allowMemorablePasswords
    ) {
        private static PasswordGeneratorPolicy personalDefault() {
            return new PasswordGeneratorPolicy(
                    MIN_PASSWORD_LENGTH,
                    MAX_PASSWORD_LENGTH,
                    false,
                    false,
                    false,
                    true
            );
        }
    }
}
