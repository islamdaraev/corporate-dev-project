package bloodbank.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Время проекта по Казахстану: у подключения приложения и у самой базы пояс Asia/Almaty (UTC+5)
@SpringBootTest(classes = Application.class)
class TimezoneTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void applicationConnectionIsInAlmaty() {
        assertEquals("Asia/Almaty", jdbc.queryForObject("SHOW timezone", String.class));
    }

    @Test
    void databaseDefaultIsAlmatyToo() {
        String settings = jdbc.queryForObject(
                "SELECT array_to_string(setconfig, ',') FROM pg_db_role_setting "
                        + "WHERE setdatabase = (SELECT oid FROM pg_database WHERE datname = current_database()) "
                        + "AND setrole = 0", String.class);
        assertTrue(settings.contains("Asia/Almaty"));
    }

    @Test
    void offsetIsPlusFive() {
        String offset = jdbc.queryForObject("SELECT to_char(now(), 'TZH')", String.class);
        assertEquals("+05", offset);
    }
}
