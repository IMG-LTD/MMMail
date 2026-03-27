package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateOrgMemberProductAccessRequest(
        @NotEmpty List<@Valid OrgProductAccessChangeRequest> products
) {
}
