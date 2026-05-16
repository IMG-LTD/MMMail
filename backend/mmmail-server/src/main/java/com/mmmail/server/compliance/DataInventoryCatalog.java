package com.mmmail.server.compliance;

import java.util.List;

@FunctionalInterface
public interface DataInventoryCatalog {

    List<DataInventoryEntry> entries();
}
