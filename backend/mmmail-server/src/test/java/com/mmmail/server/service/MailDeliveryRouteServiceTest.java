package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MailDeliveryRouteServiceTest {

    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final PassAliasService passAliasService = mock(PassAliasService.class);
    private final PassAliasContactService passAliasContactService = mock(PassAliasContactService.class);
    private final PassMailboxService passMailboxService = mock(PassMailboxService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final MailDeliveryRouteService mailDeliveryRouteService = new MailDeliveryRouteService(
            userAccountMapper,
            passAliasService,
            passAliasContactService,
            passMailboxService,
            auditService
    );

    @Test
    void resolveDeliveryTargetsRejectsMissingManagedLocalRecipient() {
        when(passAliasContactService.isOwnedEnabledAlias(1L, "sender@mmmail.local")).thenReturn(false);
        when(userAccountMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<UserAccount>>any())).thenReturn(null);
        when(passAliasService.loadEnabledAliasByEmail("missing@mmmail.local")).thenReturn(null);

        assertThatThrownBy(() -> mailDeliveryRouteService.resolveDeliveryTargets(
                1L,
                "sender@mmmail.local",
                "missing@mmmail.local",
                "127.0.0.1"
        )).isInstanceOfSatisfying(BizException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT.getCode());
            assertThat(exception).hasMessage("Unable to deliver mail");
        });

        verify(auditService).record(
                1L,
                "MAIL_SEND_REJECTED",
                "recipient not found: missing@mmmail.local",
                "127.0.0.1"
        );
        verifyNoInteractions(passMailboxService);
    }
}
