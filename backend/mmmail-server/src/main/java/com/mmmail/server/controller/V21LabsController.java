package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.labs.LabsModuleCatalog;
import com.mmmail.labs.LabsModuleDescriptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/labs")
public class V21LabsController {

    private static final String MODULE_NOT_FOUND = "Labs module does not exist";
    private static final String SETTINGS_UNSUPPORTED = "Labs module settings write is not supported by current runtime bridge";

    private final LabsModuleCatalog labsModuleCatalog;

    public V21LabsController(LabsModuleCatalog labsModuleCatalog) {
        this.labsModuleCatalog = labsModuleCatalog;
    }

    @GetMapping("/modules")
    public Result<List<LabsModuleDescriptor>> modules() {
        return Result.success(labsModuleCatalog.listModules());
    }

    @GetMapping("/modules/{moduleKey}")
    public Result<LabsModuleDescriptor> module(@PathVariable String moduleKey) {
        return Result.success(labsModuleCatalog.findModule(moduleKey)
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, MODULE_NOT_FOUND)));
    }

    @PatchMapping("/modules/{moduleKey}/settings")
    public Result<Void> patchModuleSettings(@PathVariable String moduleKey) {
        labsModuleCatalog.findModule(moduleKey)
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, MODULE_NOT_FOUND));
        throw new BizException(ErrorCode.INVALID_ARGUMENT, SETTINGS_UNSUPPORTED);
    }
}
