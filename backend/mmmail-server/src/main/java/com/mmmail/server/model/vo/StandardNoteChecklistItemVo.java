package com.mmmail.server.model.vo;

public record StandardNoteChecklistItemVo(
        int itemIndex,
        String text,
        boolean completed
) {
}
