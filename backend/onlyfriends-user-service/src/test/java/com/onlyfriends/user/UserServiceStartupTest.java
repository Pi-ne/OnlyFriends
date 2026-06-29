package com.onlyfriends.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceStartupTest {
    @LocalServerPort
    private int port;

    @Test
    void serviceStartsWithEmbeddedWebServer() {
        assertThat(port).isPositive();
    }
}
