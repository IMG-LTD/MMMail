package com.mmmail.server.service;

import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.platform.contract.V21ApiContractCatalog;
import com.mmmail.server.model.vo.V21EntitlementMatrixVo;
import com.mmmail.server.model.vo.V21EntitlementStateVo;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class V21EntitlementRuntimeBridgeService {

    private static final String COMMUNITY = "community";
    private static final String PREMIUM = "premium";
    private static final String HOSTED = "hosted";
    private static final String STATE_AVAILABLE = "available";
    private static final String STATE_LOCKED = "locked";
    private static final Set<String> PUBLIC_HELPER_MODULES = Set.of("identity", "public-share", "system");

    public List<V21EntitlementStateVo> states() {
        return productContracts().stream()
                .map(this::toState)
                .toList();
    }

    public V21EntitlementMatrixVo matrix() {
        List<V21ApiContract> contracts = productContracts();
        return new V21EntitlementMatrixVo(
                identitiesFor(contracts, COMMUNITY),
                identitiesFor(contracts, PREMIUM),
                identitiesFor(contracts, HOSTED)
        );
    }

    private List<V21ApiContract> productContracts() {
        return V21ApiContractCatalog.defaultCatalog().contracts().stream()
                .filter(this::isProductContract)
                .sorted(contractComparator())
                .toList();
    }

    private V21EntitlementStateVo toState(V21ApiContract contract) {
        String entitlement = contract.entitlement();
        return new V21EntitlementStateVo(
                contract.identity(),
                contract.ownerModule() + " " + contract.path(),
                COMMUNITY.equals(entitlement) ? STATE_AVAILABLE : STATE_LOCKED,
                COMMUNITY.equals(entitlement) ? null : entitlement
        );
    }

    private List<String> identitiesFor(List<V21ApiContract> contracts, String entitlement) {
        return contracts.stream()
                .filter(contract -> entitlement.equals(contract.entitlement()))
                .map(V21ApiContract::identity)
                .toList();
    }

    private boolean isProductContract(V21ApiContract contract) {
        return !PUBLIC_HELPER_MODULES.contains(contract.ownerModule());
    }

    private Comparator<V21ApiContract> contractComparator() {
        return Comparator.comparing(V21ApiContract::ownerModule)
                .thenComparing(V21ApiContract::path)
                .thenComparing(V21ApiContract::method);
    }
}
