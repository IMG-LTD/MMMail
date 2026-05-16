package com.mmmail.server.model.vo;

public record CommunityReactionVo(
        boolean liked,
        int likeCount
) {
}
