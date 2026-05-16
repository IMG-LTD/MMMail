package com.mmmail.server;

import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 ErrorCode.java 的段位规约（spec docs/v212-migration-spec.md §22.1，由 v2.1.3 收尾 spec T-4 引入）。
 *
 * 任何新增枚举如果落在表外段位，必须先扩 spec §22.1 + 本测试的 RANGES。
 */
class ErrorCodeRangeContractTest {

    /** 段位定义（含上下界，闭区间）。新增段位须同步 spec §22.1 与 ErrorCode.java 顶部 javadoc。 */
    private static final int[][] RANGES = {
            {10000, 19999},   // 通用 / 限流
            {20000, 29999},   // 认证 / 用户 / 会话
            {30000, 39999},   // 业务资源 NotFound / 冲突
            {40000, 40299},   // HTTP 400
            {40300, 40399},   // HTTP 403
            {40900, 40999},   // HTTP 409
            {42000, 42399},   // HTTP 422
            {42900, 42999},   // HTTP 429
            {50000, 59999},   // HTTP 5xx
            {90000, 90000}    // INTERNAL_ERROR
    };

    @Test
    void everyErrorCodeFallsIntoARegisteredRange() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(matchesAnyRange(code.getCode()))
                    .as("ErrorCode %s (code=%d) 必须落在 spec §22.1 段位表内；"
                            + "若需新段位，先扩 spec 再扩本测试 RANGES",
                            code.name(), code.getCode())
                    .isTrue();
        }
    }

    @Test
    void noTwoErrorCodesShareTheSameNumeric() {
        Set<Integer> seen = new HashSet<>();
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(seen.add(code.getCode()))
                    .as("ErrorCode %s 复用了已存在的数字码 %d", code.name(), code.getCode())
                    .isTrue();
        }
    }

    @Test
    void messagesAreNotBlank() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.getMessage())
                    .as("ErrorCode %s 的英文 message 为空", code.name())
                    .isNotBlank();
        }
    }

    private static boolean matchesAnyRange(int code) {
        for (int[] range : RANGES) {
            if (code >= range[0] && code <= range[1]) {
                return true;
            }
        }
        return false;
    }
}
