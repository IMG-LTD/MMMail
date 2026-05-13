package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.V21EntitlementMatrixVo;
import com.mmmail.server.model.vo.V21EntitlementStateVo;
import com.mmmail.server.service.V21EntitlementRuntimeBridgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/entitlements")
public class V21EntitlementsController {

    private final V21EntitlementRuntimeBridgeService entitlementRuntimeBridgeService;

    public V21EntitlementsController(V21EntitlementRuntimeBridgeService entitlementRuntimeBridgeService) {
        this.entitlementRuntimeBridgeService = entitlementRuntimeBridgeService;
    }

    @GetMapping
    public Result<List<V21EntitlementStateVo>> states() {
        return Result.success(entitlementRuntimeBridgeService.states());
    }

    @GetMapping("/matrix")
    public Result<V21EntitlementMatrixVo> matrix() {
        return Result.success(entitlementRuntimeBridgeService.matrix());
    }
}
