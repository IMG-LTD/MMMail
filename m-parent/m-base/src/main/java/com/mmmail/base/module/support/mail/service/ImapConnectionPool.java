package com.mmmail.base.module.support.mail.service;

import com.mmmail.base.module.support.mail.config.MailProperties;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class ImapConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ImapConnectionPool.class);

    private final GenericObjectPool<ImapConnection> pool;

    @Autowired
    public ImapConnectionPool(MailProperties mailProperties) {
        GenericObjectPoolConfig<ImapConnection> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(100); // 最大连接数
        config.setMaxIdle(50); // 最大空闲连接数
        config.setMinIdle(10); // 最小空闲连接数
        config.setMaxWaitMillis(10000); // 获取连接最大等待时间
        config.setTestOnBorrow(true); // 获取连接时测试有效性
        
        this.pool = new GenericObjectPool<>(new ImapConnectionFactory(mailProperties), config);
    }

    public ImapConnection getConnection(String username, String password) throws Exception {
        return pool.borrowObject();
    }

    public void returnConnection(ImapConnection connection) {
        pool.returnObject(connection);
    }

    public void close() {
        pool.close();
    }

    public int getNumActive() {
        return pool.getNumActive();
    }

    public int getNumIdle() {
        return pool.getNumIdle();
    }

    public void setMaxTotal(int maxTotal) {
        pool.setMaxTotal(maxTotal);
    }

    private static class ImapConnection {
        private final Session session;
        private final Store store;
        private final Folder inbox;
        private final String username;
        private final String host;
        private final int port;

        public ImapConnection(Session session, Store store, Folder inbox, String username, String host, int port) {
            this.session = session;
            this.store = store;
            this.inbox = inbox;
            this.username = username;
            this.host = host;
            this.port = port;
        }

        public Session getSession() {
            return session;
        }

        public Store getStore() {
            return store;
        }

        public Folder getInbox() {
            return inbox;
        }

        public void close() throws MessagingException {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }

    private static class ImapConnectionFactory extends BasePooledObjectFactory<ImapConnection> {
        private final MailProperties mailProperties;

        public ImapConnectionFactory(MailProperties mailProperties) {
            this.mailProperties = mailProperties;
        }

        @Override
        public ImapConnection create() throws Exception {
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            props.put("mail.imaps.ssl.trust", "*");
            props.put("mail.imaps.ssl.enable", "true");
            
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(mailProperties.getImap().getHost(), 
                         mailProperties.getImap().getPort(), 
                         mailProperties.getImap().getUsername(), 
                         mailProperties.getImap().getPassword());

            Folder inbox = (Folder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            return new ImapConnection(session, store, inbox, 
                                    mailProperties.getImap().getUsername(), 
                                    mailProperties.getImap().getHost(), 
                                    mailProperties.getImap().getPort());
        }

        @Override
        public PooledObject<ImapConnection> wrap(ImapConnection connection) {
            return new DefaultPooledObject<>(connection);
        }

        @Override
        public void destroyObject(PooledObject<ImapConnection> p) throws Exception {
            p.getObject().close();
        }

        @Override
        public boolean validateObject(PooledObject<ImapConnection> p) {
            try {
                return p.getObject().getStore().isConnected() && 
                       p.getObject().getInbox().isOpen();
            } catch (Exception e) {
                return false;
            }
        }
    }
}
