package bloodbank.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Профиль dev: значения берутся из application-dev.yml. В сеть тест не ходит.
@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
class DevProfileTest {

    @Autowired
    DownstreamProperties downstream;

    @Test
    void downstreamValuesComeFromTheDevYaml() {
        assertEquals("http://localhost:9090", downstream.baseUrl());
        assertEquals(Duration.ofSeconds(2), downstream.timeout());
    }
}