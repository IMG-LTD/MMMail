package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Service
public class JndiDomainDnsLookupService implements DomainDnsLookupService {

    private static final String DNS_CONTEXT_FACTORY = "com.sun.jndi.dns.DnsContextFactory";

    @Override
    public List<String> resolveTxt(String host) {
        return resolve(host, "TXT");
    }

    @Override
    public List<String> resolveCname(String host) {
        return resolve(host, "CNAME");
    }

    @Override
    public List<String> resolveMx(String host) {
        return resolve(host, "MX");
    }

    private List<String> resolve(String host, String type) {
        try {
            Attributes attributes = context().getAttributes(host, new String[]{type});
            Attribute attribute = attributes.get(type);

            return attribute == null ? List.of() : values(attribute);
        } catch (NamingException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "DNS lookup failed for " + host);
        }
    }

    private InitialDirContext context() throws NamingException {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put("java.naming.factory.initial", DNS_CONTEXT_FACTORY);

        return new InitialDirContext(environment);
    }

    private List<String> values(Attribute attribute) throws NamingException {
        List<String> values = new ArrayList<>();
        NamingEnumeration<?> all = attribute.getAll();
        while (all.hasMore()) {
            values.add(String.valueOf(all.next()).replace("\"", ""));
        }

        return values;
    }
}
