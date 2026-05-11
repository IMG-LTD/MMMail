package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.platform.contract.V21ApiContractCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/platform/contracts")
public class V21ApiContractCatalogController {

    @GetMapping
    public Result<Map<String, Object>> readContracts() {
        return Result.success(V21ApiContractCatalog.defaultCatalog().toPayload());
    }
}
