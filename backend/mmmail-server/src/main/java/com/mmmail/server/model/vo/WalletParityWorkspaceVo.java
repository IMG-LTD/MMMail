package com.mmmail.server.model.vo;

import java.util.List;

public record WalletParityWorkspaceVo(
        WalletAdvancedSettingsVo advancedSettings,
        WalletAccountProfileVo profile,
        List<WalletReceiveAddressVo> receiveAddresses,
        List<WalletReceiveAddressVo> changeAddresses,
        List<WalletEmailTransferVo> emailTransfers
) {
}
