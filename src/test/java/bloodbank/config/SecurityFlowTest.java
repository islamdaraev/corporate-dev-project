package bloodbank.config;

import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Всё по-настоящему: настоящий вход по паролю, настоящий токен, настоящий декодер и PostgreSQL.
 * Профиль dev выбирается сам (spring.profiles.default), поэтому dev-пользователи и секрет на месте.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class SecurityFlowTest {

    private static final String UNITS = "/api/v1/blood-units";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BloodUnitService service;

    @Autowired
    JwtEncoder encoder;

    @Autowired
    UserDetailsService users;

    @BeforeEach
    void emptyTable() {
        jdbc.update("DELETE FROM blood_unit");
    }

    // ---------- помощники ----------

    private MvcResult loginRequest(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = loginRequest(username, password);
        assertEquals(200, result.getResponse().getStatus());
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String technicianToken() throws Exception {
        return login("technician", "tech-dev");
    }

    private String chiefToken() throws Exception {
        return login("chief", "chief-dev");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Токен, подписанный нашим секретом, но с нужным издателем и сроком (для проверки отказов). */
    private String tokenWith(String issuer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("chief")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of("CHIEF_PHYSICIAN"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private BloodUnitId releasedUnit(String businessKey) {
        BloodUnitId id = BloodUnitId.newId();
        service.register(id, new BloodUnitNumber(businessKey), "Цельная кровь");
        jdbc.update("UPDATE blood_unit SET status = 'RELEASED' WHERE id = ?", id.value());
        return id;
    }

    private String statusInTable(BloodUnitId id) {
        return jdbc.queryForObject("SELECT status FROM blood_unit WHERE id = ?", String.class, id.value());
    }

    // ---------- вход ----------

    @Test
    void loginGivesBearerTokenForNineHundredSeconds() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"technician\",\"password\":\"tech-dev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        MvcResult wrongPassword = loginRequest("technician", "not-the-password");
        MvcResult unknownUser = loginRequest("nobody", "tech-dev");

        assertEquals(401, wrongPassword.getResponse().getStatus());
        assertEquals(401, unknownUser.getResponse().getStatus());
        for (MvcResult result : List.of(wrongPassword, unknownUser)) {
            var body = mapper.readTree(result.getResponse().getContentAsString());
            assertEquals("UNAUTHENTICATED", body.get("code").asText());
            assertEquals("bad username or password", body.get("message").asText());
            assertFalse(result.getResponse().getContentAsString().contains("tech-dev"));
        }
    }

    @Test
    void blankUsernameGives400NamingTheField() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("username must not be blank"));
    }

    @Test
    void blankPasswordGives400NamingTheField() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"technician\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password must not be blank"));
    }

    @Test
    void devPasswordsAreStoredAsBcryptHashesNotAsText() {
        String stored = users.loadUserByUsername("technician").getPassword();

        assertTrue(stored.startsWith("{bcrypt}"));
        assertFalse(stored.contains("tech-dev"));
    }

    @Test
    void aShortSecretStopsTheApplicationAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtKey("secret"));
    }

    // ---------- одна история от начала до конца ----------

    @Test
    void technicianIsForbiddenToIssueAndChiefSucceeds() throws Exception {
        String technician = technicianToken();

        // лаборант создаёт пакет и доводит его до RELEASED
        MvcResult created = mvc.perform(post(UNITS).header("Authorization", bearer(technician))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"BU-2026-000501\",\"title\":\"Цельная кровь\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post(UNITS + "/" + id + "/test").header("Authorization", bearer(technician)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TESTED"));
        mvc.perform(post(UNITS + "/" + id + "/release").header("Authorization", bearer(technician)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        // выдать он не может: 403, и строка в таблице осталась прежней
        mvc.perform(post(UNITS + "/" + id + "/issue").header("Authorization", bearer(technician)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("not allowed for your role"));
        assertEquals("RELEASED", statusInTable(new BloodUnitId(id)));

        // главврач выдаёт: успех, новый статус
        mvc.perform(post(UNITS + "/" + id + "/issue").header("Authorization", bearer(chiefToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
        assertEquals("ISSUED", statusInTable(new BloodUnitId(id)));

        // второй раз выдать нельзя: правила, не 200, строка не меняется
        mvc.perform(post(UNITS + "/" + id + "/issue").header("Authorization", bearer(chiefToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
        assertEquals("ISSUED", statusInTable(new BloodUnitId(id)));
    }

    @Test
    void chiefCannotIssueACollectedUnitAndTheRowStaysTheSame() throws Exception {
        BloodUnitId id = BloodUnitId.newId();
        service.register(id, new BloodUnitNumber("BU-2026-000502"), "Свежий пакет");

        mvc.perform(post(UNITS + "/" + id + "/issue").header("Authorization", bearer(chiefToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));

        assertEquals("COLLECTED", statusInTable(id));
    }

    // ---------- проверка токена на каждом запросе ----------

    @Test
    void meReadsTheNameAndTheRoleFromTheRealToken() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", bearer(chiefToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chief"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CHIEF_PHYSICIAN"));
    }

    @Test
    void tokenWithEditedRoleIsRefusedBecauseTheSignatureDoesNotMatch() throws Exception {
        String[] parts = technicianToken().split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forged = payload.replace("LAB_TECHNICIAN", "CHIEF_PHYSICIAN");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes(StandardCharsets.UTF_8));
        String forgedToken = parts[0] + "." + forgedPayload + "." + parts[2];

        mvc.perform(get("/api/me").header("Authorization", bearer(forgedToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("valid token required"));
    }

    @Test
    void expiredTokenIsRefused() throws Exception {
        String expired = tokenWith("bloodbank", Instant.now().minus(Duration.ofHours(2)));

        mvc.perform(get("/api/me").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfAnotherIssuerIsRefused() throws Exception {
        String foreign = tokenWith("helpdesk", Instant.now().plus(Duration.ofMinutes(15)));

        mvc.perform(get("/api/me").header("Authorization", bearer(foreign)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notAJwtAtAllIsRefused() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer hello"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- второй замок: метод сервиса ----------

    @Test
    @WithMockUser(roles = "LAB_TECHNICIAN")
    void methodLockStopsTheTechnicianEvenWithoutTheUrlRule() {
        BloodUnitId id = releasedUnit("BU-2026-000503");

        assertThrows(AccessDeniedException.class, () -> service.issue(id, "technician"));

        assertEquals("RELEASED", statusInTable(id));
    }

    // ---------- открытые адреса ----------

    @Test
    void healthIsOpenWithoutToken() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerSpecIsOpenAndNamesTheStatusesOfTheLockedVerb() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/blood-units/{id}/issue'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/blood-units/{id}/issue'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/blood-units/{id}/issue'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get.responses['401']").exists());
    }

    @Test
    void swaggerPageIsOpenWithoutToken() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void envAndHeapdumpStayClosed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/heapdump")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env").header("Authorization", bearer(chiefToken())))
                .andExpect(status().isForbidden());
    }
}
