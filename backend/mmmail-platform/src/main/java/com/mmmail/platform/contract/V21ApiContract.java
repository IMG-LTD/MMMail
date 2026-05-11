package com.mmmail.platform.contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record V21ApiContract(String method, String path, V21ApiContractMetadata metadata) {

    public String identity() {
        return method + " " + path;
    }

    public String ownerModule() {
        return metadata.owner().module();
    }

    public String responseModel() {
        return metadata.schema().responseModel();
    }

    public String requestModel() {
        return metadata.schema().requestModel();
    }

    public List<String> permissions() {
        return metadata.access().permissions();
    }

    public String entitlement() {
        return metadata.access().entitlement();
    }

    public String designSource() {
        return metadata.owner().designSource();
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", method);
        payload.put("path", path);
        payload.put("ownerModule", ownerModule());
        payload.put("responseModel", responseModel());
        payload.put("requestModel", requestModel());
        payload.put("permissions", permissions());
        payload.put("entitlement", entitlement());
        payload.put("designSource", designSource());
        return payload;
    }
}
