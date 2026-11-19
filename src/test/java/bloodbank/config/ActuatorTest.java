package bloodbank.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Лаба 5: из actuator без токена открыт только /actuator/health (строка таблицы прав).
 * Остальное закрыто цепочкой фильтров: сначала 401, до самого actuator запрос не доходит.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ActuatorTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void infoIsClosedWithoutToken() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void envAndHeapdumpAreNotAvailable() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/heapdump")).andExpect(status().isUnauthorized());
    }
}
