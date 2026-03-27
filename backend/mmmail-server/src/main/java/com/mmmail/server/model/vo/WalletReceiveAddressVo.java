package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletReceiveAddressVo(
        String addressId,
        String address,
        String label,
        String sourceType,
        String addressKind,
        int addressIndex,
        String addressStatus,
        long valueMinor,
        String reservedFor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
