package com.mmmail.server.service;

import java.util.List;

public interface DomainDnsLookupService {

    List<String> resolveTxt(String host);

    List<String> resolveCname(String host);

    List<String> resolveMx(String host);
}
