package com.mmmail.server.commercial;

public enum Edition {
    FREE(0),
    PRO(1),
    BUSINESS(2);

    private final int rank;

    Edition(int rank) {
        this.rank = rank;
    }

    public boolean allows(Edition requiredEdition) {
        return rank >= requiredEdition.rank;
    }
}
