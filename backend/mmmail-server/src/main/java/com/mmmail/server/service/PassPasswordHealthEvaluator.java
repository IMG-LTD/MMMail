package com.mmmail.server.service;

import com.mmmail.server.model.entity.PassVaultItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class PassPasswordHealthEvaluator {

    private static final int MIN_ACCEPTABLE_LENGTH = 10;
    private static final int RECOMMENDED_LENGTH = 14;
    private static final int MIN_COMPLEXITY_GROUPS = 3;
    private static final int SINGLE_GROUP_MAX_LENGTH = 16;
    private static final Set<String> MONITORABLE_ITEM_TYPES = Set.of(
            PassBusinessConstants.ITEM_TYPE_LOGIN,
            PassBusinessConstants.ITEM_TYPE_PASSWORD
    );

    public boolean supportsMonitoring(PassVaultItem item) {
        return item != null
                && MONITORABLE_ITEM_TYPES.contains(item.getItemType())
                && StringUtils.hasText(item.getSecretCiphertext());
    }

    public boolean isWeakSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            return false;
        }
        int complexityGroups = countComplexityGroups(secret);
        if (secret.length() < MIN_ACCEPTABLE_LENGTH) {
            return true;
        }
        if (complexityGroups <= 1 && secret.length() <= SINGLE_GROUP_MAX_LENGTH) {
            return true;
        }
        return secret.length() < RECOMMENDED_LENGTH && complexityGroups < MIN_COMPLEXITY_GROUPS;
    }

    private int countComplexityGroups(String secret) {
        int count = 0;
        if (secret.chars().anyMatch(Character::isLowerCase)) {
            count++;
        }
        if (secret.chars().anyMatch(Character::isUpperCase)) {
            count++;
        }
        if (secret.chars().anyMatch(Character::isDigit)) {
            count++;
        }
        if (secret.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            count++;
        }
        return count;
    }
}
