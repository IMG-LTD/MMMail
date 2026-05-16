package com.mmmail.server.model.vo;

import java.util.List;

public record CommunityPostPageVo(
        List<CommunityPostVo> items,
        long total,
        int page,
        int size
) {
}
