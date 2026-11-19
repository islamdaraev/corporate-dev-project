package bloodbank.handler;

import bloodbank.config.Application;
import bloodbank.config.BloodUnitService;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.BloodUnitView;
import bloodbank.domain.ForbiddenTransition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static bloodbank.handler.TestTokens.chief;
import static bloodbank.handler.TestTokens.technician;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Строки таблицы прав из README, по одной на тест. Цепочка фильтров настоящая, сервис замокан.
 * Главное в каждом "нельзя": сервис не вызван, значит до таблицы и до больницы дело не дошло.
 */
@WebMvcTest({BloodUnitController.class, MeController.class})
@ContextConfiguration(classes = Application.class)
@Import(SecurityConfig.class)
class SecurityRulesTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String UNIT = "/api/v1/blood-units/" + ID;

    @Autowired
    MockMvc mvc;

    @MockBean
    BloodUnitService service;

    @MockBean
    JwtDecoder jwtDecoder;

    private static BloodUnitView issuedUnit() {
        return new BloodUnitView(new BloodUnitId(ID.toString()), new BloodUnitNumber("BU-2026-000001"),
                BloodUnitStatus.ISSUED, "Цельная кровь", null, null);
    }

    // ---------- нет токена: 401 ----------

    @Test
    void readWithoutTokenGives401AndServiceIsNotCalled() throws Exception {
        mvc.perform(get(UNIT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void createWithoutTokenGives401NotCreated() throws Exception {
        mvc.perform(post("/api/v1/blood-units").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"BU-2026-000001\",\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(service);
    }

    @Test
    void issueWithoutTokenGives401AndServiceIsNotCalled() throws Exception {
        mvc.perform(post(UNIT + "/issue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(service);
    }

    @Test
    void garbageTokenGives401() throws Exception {
        when(jwtDecoder.decode(any())).thenThrow(new BadJwtException("not a jwt"));

        mvc.perform(get(UNIT).header("Authorization", "Bearer hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"));

        verifyNoInteractions(service);
    }

    @Test
    void correlationIdIsEchoedIn401() throws Exception {
        mvc.perform(get(UNIT).header("X-Correlation-Id", "desk-4421"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").value("desk-4421"));
    }

    // ---------- закрытое действие: лаборант 403, главврач успех ----------

    @Test
    void technicianCannotIssueGives403AndServiceIsNotCalled() throws Exception {
        mvc.perform(post(UNIT + "/issue").with(technician()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("not allowed for your role"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void correlationIdIsEchoedIn403() throws Exception {
        mvc.perform(post(UNIT + "/issue").with(technician()).header("X-Correlation-Id", "desk-4422"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.correlationId").value("desk-4422"));
    }

    @Test
    void chiefCanIssueAndTheNameComesFromTheTokenNotFromTheBody() throws Exception {
        when(service.issue(any(BloodUnitId.class), eq("chief"))).thenReturn(issuedUnit());

        // в теле пытаемся подсунуть чужое имя и роль: контроллер их вообще не читает
        mvc.perform(post(UNIT + "/issue").with(chief())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"hacker\",\"role\":\"CHIEF_PHYSICIAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));

        verify(service).issue(any(BloodUnitId.class), eq("chief"));
    }

    @Test
    void policyForbidsTheMoveGives409AndNotOk() throws Exception {
        when(service.issue(any(BloodUnitId.class), any()))
                .thenThrow(new ForbiddenTransition("Нельзя перевести пакет из COLLECTED в ISSUED"));

        mvc.perform(post(UNIT + "/issue").with(chief()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
    }

    @Test
    void methodLockInTheServiceGives403NotInternalError() throws Exception {
        // второй замок (@PreAuthorize) бросает AccessDeniedException уже внутри контроллера
        when(service.issue(any(BloodUnitId.class), any())).thenThrow(new AccessDeniedException("method lock"));

        mvc.perform(post(UNIT + "/issue").with(chief()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ---------- действия, которые доступны обеим ролям ----------

    @Test
    void technicianCanSendToTestAndTheNameComesFromTheToken() throws Exception {
        when(service.sendToTest(any(BloodUnitId.class), eq("technician"))).thenReturn(issuedUnit());

        mvc.perform(post(UNIT + "/test").with(technician()))
                .andExpect(status().isOk());

        verify(service).sendToTest(any(BloodUnitId.class), eq("technician"));
    }

    @Test
    void technicianCanRelease() throws Exception {
        when(service.release(any(BloodUnitId.class), eq("technician"))).thenReturn(issuedUnit());

        mvc.perform(post(UNIT + "/release").with(technician()))
                .andExpect(status().isOk());
    }

    @Test
    void readWithTechnicianTokenReachesTheService() throws Exception {
        when(service.require(any(BloodUnitId.class))).thenReturn(issuedUnit());

        mvc.perform(get(UNIT).with(technician()))
                .andExpect(status().isOk());

        verify(service).require(any(BloodUnitId.class));
    }

    // ---------- GET /api/me ----------

    @Test
    void meReturnsTheNameAndTheRoleFromTheToken() throws Exception {
        mvc.perform(get("/api/me").with(technician()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("technician"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_LAB_TECHNICIAN"));
    }

    @Test
    void meWithoutTokenGives401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // ---------- всё остальное запрещено ----------

    @Test
    void anythingElseWithATokenGives403() throws Exception {
        mvc.perform(get("/something-else").with(chief()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void anythingElseWithoutTokenGives401() throws Exception {
        mvc.perform(get("/something-else"))
                .andExpect(status().isUnauthorized());
    }
}
