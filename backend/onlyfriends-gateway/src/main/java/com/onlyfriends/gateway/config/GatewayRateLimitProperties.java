package com.onlyfriends.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {
    private boolean enabled = false;
    private int permitsPerSecond = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPermitsPerSecond() {
        return permitsPerSecond;
    }

    public void setPermitsPerSecond(int permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }
}
