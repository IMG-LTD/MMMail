package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateSimpleLoginRelayPolicyRequest;
import com.mmmail.server.model.dto.UpdateSimpleLoginRelayPolicyRequest;
import com.mmmail.server.model.vo.SimpleLoginOverviewVo;
import com.mmmail.server.model.vo.SimpleLoginRelayPolicyVo;
import com.mmmail.server.service.SimpleLoginRelayPolicyService;
import com.mmmail.server.service.SimpleLoginService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simplelogin")
public class SimpleLoginController {

    private final SimpleLoginService simpleLoginService;
    private final SimpleLoginRelayPolicyService simpleLoginRelayPolicyService;

    public SimpleLoginController(
            SimpleLoginService simpleLoginService,
            SimpleLoginRelayPolicyService simpleLoginRelayPolicyService
    ) {
        this.simpleLoginService = simpleLoginService;
        this.simpleLoginRelayPolicyService = simpleLoginRelayPolicyService;
    }

    @GetMapping("/overview")
    public Result<SimpleLoginOverviewVo> getOverview(
            @RequestParam(required = false) Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(simpleLoginService.getOverview(
                SecurityUtils.currentUserId(),
                orgId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/relay-policies")
    public Result<List<SimpleLoginRelayPolicyVo>> listRelayPolicies(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(simpleLoginRelayPolicyService.listPolicies(
                SecurityUtils.currentUserId(),
                orgId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/relay-policies")
    public Result<SimpleLoginRelayPolicyVo> createRelayPolicy(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateSimpleLoginRelayPolicyRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(simpleLoginRelayPolicyService.createPolicy(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/orgs/{orgId}/relay-policies/{policyId}")
    public Result<SimpleLoginRelayPolicyVo> updateRelayPolicy(
            @PathVariable Long orgId,
            @PathVariable Long policyId,
            @Valid @RequestBody UpdateSimpleLoginRelayPolicyRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(simpleLoginRelayPolicyService.updatePolicy(
                SecurityUtils.currentUserId(),
                orgId,
                policyId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/orgs/{orgId}/relay-policies/{policyId}")
    public Result<Void> deleteRelayPolicy(
            @PathVariable Long orgId,
            @PathVariable Long policyId,
            HttpServletRequest httpRequest
    ) {
        simpleLoginRelayPolicyService.deletePolicy(
                SecurityUtils.currentUserId(),
                orgId,
                policyId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }
}
