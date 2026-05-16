package com.mmmail.server.model.vo;

import java.util.List;

public record V21CollaborationBoardColumnVo(
        String columnId,
        String title,
        List<V21CollaborationTaskVo> tasks
) {
}
