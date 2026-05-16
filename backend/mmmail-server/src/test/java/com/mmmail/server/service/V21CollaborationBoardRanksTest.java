package com.mmmail.server.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class V21CollaborationBoardRanksTest {

    @Test
    void shouldGenerateLexorankCompatiblePositionsAtFixedIntervals() {
        assertThat(V21CollaborationBoardRanks.atIndex(0)).isEqualTo("0|i001000:");
        assertThat(V21CollaborationBoardRanks.atIndex(2)).isEqualTo("0|i003000:");
    }

    @Test
    void shouldAppendAfterLexorankAndLegacyNumericPositions() {
        assertThat(V21CollaborationBoardRanks.after("0|i001000:")).isEqualTo("0|i002000:");
        assertThat(V21CollaborationBoardRanks.after("00000000000000001000")).isEqualTo("0|i002000:");
    }
}
