package com.mmmail.server;

import com.mmmail.server.service.VapidWebPushDeliveryGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VapidWebPushDeliveryGatewayConfigurationTest {

    private static final String VAPID_PUBLIC_KEY = "BI5HRQRgOlgDr8obEjZxNN8kW6h7423oR_20aeAFZDohqrMkCr1z6f73GgYYKKVhGrRaMbrVFlvPkfwjHEASSHs";
    private static final String VAPID_PRIVATE_KEY = "jAsHKPkR7ktlNMJQecBAqChiZl-FLIHdpMXn8T3OkqI";

    @Test
    void shouldInitializeGatewayWithValidVapidKeyPair() {
        VapidWebPushDeliveryGateway gateway = new VapidWebPushDeliveryGateway(
                VAPID_PUBLIC_KEY,
                VAPID_PRIVATE_KEY,
                "mailto:test@mmmail.local"
        );

        assertThat(gateway.isConfigured()).isTrue();
        assertThat(gateway.configurationMessage()).isNull();
        assertThat(gateway.publicKey()).isEqualTo(VAPID_PUBLIC_KEY);
    }
}
