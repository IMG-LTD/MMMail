package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record StandardNotesExportVo(
        String fileName,
        String format,
        LocalDateTime exportedAt,
        StandardNotesOverviewVo overview,
        List<StandardNoteFolderVo> folders,
        List<StandardNoteDetailVo> notes
) {
}
