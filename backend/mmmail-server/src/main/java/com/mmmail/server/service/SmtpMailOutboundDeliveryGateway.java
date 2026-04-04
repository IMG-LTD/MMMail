package com.mmmail.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Component
public class SmtpMailOutboundDeliveryGateway implements MailOutboundDeliveryGateway {

    private static final String DISABLED_MESSAGE = "SMTP outbound is disabled";
    private static final String INCOMPLETE_CONFIG_MESSAGE = "SMTP outbound configuration is incomplete";
    private static final int DEFAULT_PORT = 587;

    private final String configurationMessage;
    private final JavaMailSenderImpl mailSender;

    public SmtpMailOutboundDeliveryGateway(
            @Value("${mmmail.smtp-outbound.enabled:false}") boolean enabled,
            @Value("${mmmail.smtp-outbound.host:}") String host,
            @Value("${mmmail.smtp-outbound.port:587}") int port,
            @Value("${mmmail.smtp-outbound.username:}") String username,
            @Value("${mmmail.smtp-outbound.password:}") String password,
            @Value("${mmmail.smtp-outbound.protocol:smtp}") String protocol,
            @Value("${mmmail.smtp-outbound.starttls-enabled:true}") boolean starttlsEnabled,
            @Value("${mmmail.smtp-outbound.ssl-enabled:false}") boolean sslEnabled,
            @Value("${mmmail.smtp-outbound.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${mmmail.smtp-outbound.read-timeout-ms:10000}") int readTimeoutMs,
            @Value("${mmmail.smtp-outbound.write-timeout-ms:10000}") int writeTimeoutMs
    ) {
        if (!enabled) {
            this.mailSender = null;
            this.configurationMessage = DISABLED_MESSAGE;
            return;
        }
        String normalizedHost = normalize(host);
        String normalizedProtocol = normalize(protocol);
        if (!StringUtils.hasText(normalizedHost) || !StringUtils.hasText(normalizedProtocol)) {
            this.mailSender = null;
            this.configurationMessage = INCOMPLETE_CONFIG_MESSAGE;
            return;
        }
        JavaMailSenderImpl createdSender = new JavaMailSenderImpl();
        createdSender.setHost(normalizedHost);
        createdSender.setPort(port > 0 ? port : DEFAULT_PORT);
        createdSender.setProtocol(normalizedProtocol);
        createdSender.setUsername(normalize(username));
        createdSender.setPassword(normalize(password));
        Properties properties = createdSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", normalizedProtocol);
        properties.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(createdSender.getUsername())));
        properties.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnabled));
        properties.put("mail.smtp.ssl.enable", String.valueOf(sslEnabled));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(connectTimeoutMs));
        properties.put("mail.smtp.timeout", String.valueOf(readTimeoutMs));
        properties.put("mail.smtp.writetimeout", String.valueOf(writeTimeoutMs));
        this.mailSender = createdSender;
        this.configurationMessage = null;
    }

    @Override
    public boolean isConfigured() {
        return mailSender != null;
    }

    @Override
    public String configurationMessage() {
        return configurationMessage;
    }

    @Override
    public MailOutboundDeliveryResult send(MailOutboundRequest request) {
        if (mailSender == null) {
            return new MailOutboundDeliveryResult(false, configurationMessage);
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(request.fromEmail());
            message.setTo(request.toEmail());
            message.setSubject(request.subject());
            message.setText(request.body());
            mailSender.send(message);
            return new MailOutboundDeliveryResult(true, "SMTP outbound delivered");
        } catch (MailException exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            return new MailOutboundDeliveryResult(false, message);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
