package com.mmmail.server.model.vo;

import java.util.List;

public record CommunityReportPageVo(
        List<CommunityReportVo> items,
        long total,
        int page,
        int size
) {
}
