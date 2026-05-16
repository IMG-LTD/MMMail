package com.mmmail.server.model.vo;

import java.util.List;

public record V21CollaborationBoardVo(
        String projectId,
        List<V21CollaborationBoardColumnVo> columns
) {
}
