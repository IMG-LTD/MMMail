package com.mmmail.server.model.vo;

import java.util.List;

public record V21EntitlementMatrixVo(
        List<String> community,
        List<String> premium,
        List<String> hosted
) {
    public V21EntitlementMatrixVo {
        community = List.copyOf(community);
        premium = List.copyOf(premium);
        hosted = List.copyOf(hosted);
    }
}
