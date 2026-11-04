package bloodbank.handler;

import bloodbank.config.Application;
import bloodbank.config.BloodUnitService;
import bloodbank.domain.BloodGroup;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.BloodUnitView;
import bloodbank.domain.DuplicateBloodUnit;
import bloodbank.domain.MissingBloodUnit;
import bloodbank.domain.RhFactor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Только веб-слой: без PostgreSQL и без настоящего сервера. Сервис заменён моком.
@WebMvcTest(BloodUnitController.class)
@ContextConfiguration(classes = Application.class)
class BloodUnitControllerTest {

    private static final String URL = "/api/v1/blood-units";

    @Autowired
    MockMvc mvc;

    @MockBean
    BloodUnitService service;

    private static String body(String businessKey, String title) {
        return "{\"businessKey\":\"" + businessKey + "\",\"title\":\"" + title + "\"}";
    }

    private static BloodUnitView unit(BloodUnitId id, String title, BloodGroup group, RhFactor rh) {
        return new BloodUnitView(id, new BloodUnitNumber("BU-2026-000001"), BloodUnitStatus.COLLECTED, title, group, rh);
    }

    @Test
    void validBodyGives201WithLocationAndStatus() throws Exception {
        BloodUnitId id = BloodUnitId.newId();
        when(service.register(any(BloodUnitId.class), any(BloodUnitNumber.class), eq("Первая заявка"), any(), any()))
                .thenReturn(unit(id, "Первая заявка", null, null));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("BU-2026-000001", "Первая заявка")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", URL + "/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.businessKey").value("BU-2026-000001"))
                .andExpect(jsonPath("$.status").value("COLLECTED"))
                .andExpect(jsonPath("$.title").value("Первая заявка"));
    }

    @Test
    void bodyWithoutGroupAndRhDoesNotShowThemInTheAnswer() throws Exception {
        when(service.register(any(BloodUnitId.class), any(BloodUnitNumber.class), any(), isNull(), isNull()))
                .thenReturn(unit(BloodUnitId.newId(), "Первая заявка", null, null));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("BU-2026-000001", "Первая заявка")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodGroup").doesNotExist())
                .andExpect(jsonPath("$.rhFactor").doesNotExist());
    }

    @Test
    void groupAndRhFromTheBodyReachTheServiceAndComeBack() throws Exception {
        when(service.register(any(BloodUnitId.class), any(BloodUnitNumber.class), any(), any(), any()))
                .thenReturn(unit(BloodUnitId.newId(), "Цельная кровь", BloodGroup.A, RhFactor.POSITIVE));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"BU-2026-000001\",\"title\":\"Цельная кровь\","
                                + "\"bloodGroup\":\"A\",\"rhFactor\":\"POSITIVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodGroup").value("A"))
                .andExpect(jsonPath("$.rhFactor").value("POSITIVE"));

        verify(service).register(any(BloodUnitId.class), any(BloodUnitNumber.class), eq("Цельная кровь"),
                eq(BloodGroup.A), eq(RhFactor.POSITIVE));
    }

    @Test
    void unknownBloodGroupGives400ListingTheAllowedWords() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"BU-2026-000001\",\"title\":\"x\",\"bloodGroup\":\"Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("bloodGroup must be one of [O, A, B, AB]"));

        verifyNoInteractions(service);
    }

    @Test
    void unknownRhFactorGives400ListingTheAllowedWords() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"BU-2026-000001\",\"title\":\"x\",\"rhFactor\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("rhFactor must be one of [POSITIVE, NEGATIVE]"));
    }

    @Test
    void blankTitleGives400NamingTheFieldAndServiceIsNotCalled() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("BU-2026-000001", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("title must not be blank"));

        verifyNoInteractions(service);
    }

    @Test
    void titleOfOnlySpacesGives400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("BU-2026-000001", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title must not be blank"));
    }

    @Test
    void blankBusinessKeyGives400NamingTheField() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("", "Первая заявка")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("businessKey must not be blank"));
    }

    @Test
    void titleOf121CharactersGives400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("BU-2026-000001", "x".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title size must be between 0 and 120"));
    }

    @Test
    void titleOf120CharactersIsAccepted() throws Exception {
        String title = "x".repeat(120);
        when(service.register(any(BloodUnitId.class), any(BloodUnitNumber.class), eq(title), any(), any()))
                .thenReturn(unit(BloodUnitId.newId(), title, null, null));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("BU-2026-000001", title)))
                .andExpect(status().isCreated());
    }

    @Test
    void missingUnitGives404NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.require(any(BloodUnitId.class))).thenThrow(new MissingBloodUnit(id.toString()));

        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void existingUnitGives200() throws Exception {
        BloodUnitId id = BloodUnitId.newId();
        when(service.require(any(BloodUnitId.class)))
                .thenReturn(unit(id, "Первая заявка", BloodGroup.O, RhFactor.NEGATIVE));

        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("COLLECTED"))
                .andExpect(jsonPath("$.bloodGroup").value("O"))
                .andExpect(jsonPath("$.rhFactor").value("NEGATIVE"));
    }

    @Test
    void duplicateBusinessKeyGives409Duplicate() throws Exception {
        when(service.register(any(BloodUnitId.class), any(BloodUnitNumber.class), any(), any(), any()))
                .thenThrow(new DuplicateBloodUnit("BU-2026-000001"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("BU-2026-000001", "Ещё раз")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void businessKeyInThePathGives400WithUuidMessage() throws Exception {
        mvc.perform(get(URL + "/BU-2026-000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("id must be a UUID"));
    }

    @Test
    void bodyThatIsNotJsonGives400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("это не json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("body must be JSON"));
    }

    @Test
    void wrongContentTypeGives415() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED"));
    }

    @Test
    void correlationIdFromTheRequestComesBackInTheErrorBody() throws Exception {
        mvc.perform(post(URL).header("X-Correlation-Id", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON).content(body("BU-2026-000001", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }

    @Test
    void missingCorrelationIdIsMintedByTheService() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("BU-2026-000001", "")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void unexpectedErrorGives500WithoutDetails() throws Exception {
        when(service.require(any(BloodUnitId.class))).thenThrow(new IllegalStateException("секретная деталь"));

        mvc.perform(get(URL + "/" + UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL"))
                .andExpect(jsonPath("$.message").value("unexpected error"));
    }
}
