package com.mmmail.server.service;

final class V21CollaborationBoardRanks {

    private static final String POSITION_PREFIX = "0|i";
    private static final String POSITION_SUFFIX = ":";
    private static final int POSITION_WIDTH = 6;
    private static final long POSITION_STEP = 1_000L;

    private V21CollaborationBoardRanks() {
    }

    static String atIndex(int index) {
        return format((long) (index + 1) * POSITION_STEP);
    }

    static String after(String position) {
        return format(rank(position) + POSITION_STEP);
    }

    static long rank(String position) {
        if (position == null || position.isBlank()) {
            return 0;
        }
        String normalized = position.trim();
        if (normalized.startsWith(POSITION_PREFIX) && normalized.endsWith(POSITION_SUFFIX)) {
            int start = POSITION_PREFIX.length();
            int end = normalized.length() - POSITION_SUFFIX.length();
            return Long.parseLong(normalized.substring(start, end));
        }
        return Long.parseLong(normalized);
    }

    private static String format(long rank) {
        return POSITION_PREFIX + String.format("%0" + POSITION_WIDTH + "d", rank) + POSITION_SUFFIX;
    }
}
