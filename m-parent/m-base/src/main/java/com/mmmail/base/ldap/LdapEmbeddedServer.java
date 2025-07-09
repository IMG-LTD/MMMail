package com.mmmail.base.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * 基于 UnboundID 的嵌入式 LDAP 服务实现
 */
@Component
public class LdapEmbeddedServer {
    private static final Logger log = LoggerFactory.getLogger(LdapEmbeddedServer.class);

    @Value("${ldap.embedded.enabled:true}")
    private boolean enabled;

    @Value("${ldap.embedded.port:1389}")
    private int port;

    @Value("${ldap.embedded.base-dn:dc=mmmail,dc=com}")
    private String baseDn;

    @Value("${ldap.embedded.admin-dn:cn=admin,dc=mmmail,dc=com}")
    private String adminDn;

    @Value("${ldap.embedded.admin-password:admin}")
    private String adminPassword;

    @Value("${ldap.embedded.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${ldap.embedded.ssl.port:1636}")
    private int sslPort;

    @Value("${ldap.embedded.ssl.keystore:}")
    private String keystorePath;

    @Value("${ldap.embedded.ssl.keystore-password:}")
    private String keystorePassword;

    private InMemoryDirectoryServer directoryServer;

    /**
     * 启动嵌入式LDAP服务（支持SSL）
     * Start the embedded LDAP server (with optional SSL support)
     */
    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("嵌入式LDAP服务已通过配置禁用。Embedded LDAP server is disabled by configuration.");
            return;
        }
        try {
            InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDn);
            InMemoryListenerConfig[] listenerConfigs;
            if (sslEnabled) {
                SSLServerSocketFactory sslServerSocketFactory = null;
                boolean sslReady = false;
                // 检查证书文件是否存在 Check if keystore file exists
                java.io.File ksFile = new java.io.File(keystorePath);
                if (ksFile.exists() && ksFile.isFile()) {
                    try {
                        SSLUtil serverSSLUtil = new SSLUtil(
                                new KeyStoreKeyManager(keystorePath, keystorePassword.toCharArray(), "JKS", null),
                                new TrustStoreTrustManager(keystorePath, keystorePassword.toCharArray(), "JKS", true));
                        sslServerSocketFactory = serverSSLUtil.createSSLServerSocketFactory();
                        sslReady = true;
                        log.info("已检测到SSL证书，SSL端口{}将启用。SSL keystore detected, SSL port {} will be enabled.", sslPort, sslPort);
                    } catch (Exception e) {
                        log.warn("SSL证书加载失败，SSL端口将不会启用。Failed to load SSL keystore, SSL port will not be enabled.", e);
                    }
                } else {
                    log.warn("未检测到SSL证书文件({})，SSL端口将不会启用。SSL keystore file ({}) not found, SSL port will not be enabled.", keystorePath, keystorePath);
                }
                if (sslReady && sslServerSocketFactory != null) {
                    listenerConfigs = new InMemoryListenerConfig[] {
                            InMemoryListenerConfig.createLDAPConfig("default", port),
                            InMemoryListenerConfig.createLDAPSConfig("ssl", null, sslPort, sslServerSocketFactory, null)
                    };
                } else {
                    listenerConfigs = new InMemoryListenerConfig[] {
                            InMemoryListenerConfig.createLDAPConfig("default", port)
                    };
                }
            } else {
                listenerConfigs = new InMemoryListenerConfig[] {
                        InMemoryListenerConfig.createLDAPConfig("default", port)
                };
            }
            config.setListenerConfigs(listenerConfigs);
            config.addAdditionalBindCredentials(adminDn, adminPassword);
            config.setSchema(Schema.getDefaultStandardSchema());
            config.setEnforceSingleStructuralObjectClass(false);
            config.setEnforceAttributeSyntaxCompliance(false);
            directoryServer = new InMemoryDirectoryServer(config);
            // 初始化根条目 Initialize root entry
            Entry baseEntry = new Entry(
                baseDn,
                new Attribute("objectClass", "top", "domain"),
                new Attribute("dc", "mmmail")
            );
            directoryServer.add(baseEntry);
            log.info("嵌入式LDAP服务启动成功，端口：{}，Base DN：{}{}。Embedded LDAP server started on port {} with base DN {}{}.", port, baseDn, sslEnabled ? (", SSL port: " + sslPort) : "", port, baseDn, sslEnabled ? (", SSL port: " + sslPort) : "");
            directoryServer.startListening();
        } catch (Exception e) {
            log.error("嵌入式LDAP服务启动失败。Failed to start embedded LDAP server.", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (directoryServer != null) {
            directoryServer.shutDown(true);
            log.info("Embedded LDAP server stopped.");
        }
    }
} 