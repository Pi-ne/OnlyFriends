package com.onlyfriends.activity;

import com.onlyfriends.activity.service.UserClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActivityServiceStartupTest {
    @LocalServerPort
    private int port;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserClient userClient;

    @Test
    void serviceStartsWithEmbeddedWebServer() {
        assertThat(port).isPositive();
        assertThat(context).isNotNull();
    }
}
