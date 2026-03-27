package com.mmmail.server.model.vo;

import java.util.List;

public record ContactDuplicateGroupVo(
        String signature,
        int count,
        List<ContactItemVo> contacts
) {
}
