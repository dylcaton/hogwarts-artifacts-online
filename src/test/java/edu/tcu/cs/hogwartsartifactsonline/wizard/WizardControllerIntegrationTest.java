package edu.tcu.cs.hogwartsartifactsonline.wizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.hogwartsartifactsonline.system.StatusCode;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration tests for Wizard API endpoints")
@Tag("integration")
@ActiveProfiles(value="dev")
class WizardControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    String token;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        ResultActions actions =
                this.mockMvc.perform(
                        post(this.baseUrl + "/users/login")
                                .with(httpBasic("john", "123456"))
                );

        MvcResult result = actions.andDo(print()).andReturn();
        String response = result.getResponse().getContentAsString();
        JSONObject jsonObj = new JSONObject(response);

        this.token = "Bearer " + jsonObj.getJSONObject("data").getString("token");
    }

    @Test
    @DisplayName("Check findAllWizards (GET)")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testFindAllWizardsSuccess() throws Exception {
        mockMvc.perform(
                        get(baseUrl + "/wizards")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find All Success"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    @DisplayName("Check findWizardById (GET)")
    void testFindWizardByIdSuccess() throws Exception {
        mockMvc.perform(
                        get(baseUrl + "/wizards/1")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find One Success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Albus Dumbledore"));
    }

    @Test
    @DisplayName("Check findWizardById with non-existent id (GET)")
    void testFindWizardByIdNotFound() throws Exception {
        mockMvc.perform(
                        get(baseUrl + "/wizards/5")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND))
                .andExpect(jsonPath("$.message").value("Could not find wizard with Id 5 :("))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Check addWizard with valid input (POST)")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testAddWizardSuccess() throws Exception {
        Wizard wizard = new Wizard();
        wizard.setName("Hermione Granger");

        String json = objectMapper.writeValueAsString(wizard);

        mockMvc.perform(
                        post(baseUrl + "/wizards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add Success"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Hermione Granger"));

        mockMvc.perform(
                        get(baseUrl + "/wizards")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.data", Matchers.hasSize(4)));
    }

    @Test
    @DisplayName("Check addWizard with invalid input (POST)")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testAddWizardErrorWithInvalidInput() throws Exception {
        Wizard wizard = new Wizard();
        wizard.setName("");

        String json = objectMapper.writeValueAsString(wizard);

        mockMvc.perform(
                        post(baseUrl + "/wizards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("Provided arguments are invalid, see data for details."))
                .andExpect(jsonPath("$.data.name").value("name is required."));
    }

    @Test
    @DisplayName("Check updateWizard with valid input (PUT)")
    void testUpdateWizardSuccess() throws Exception {
        Wizard wizard = new Wizard();
        wizard.setId(1);
        wizard.setName("Updated wizard name");

        String json = objectMapper.writeValueAsString(wizard);

        mockMvc.perform(
                        put(baseUrl + "/wizards/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.name").value("Updated wizard name"));
    }

    @Test
    @DisplayName("Check deleteWizard with valid input (DELETE)")
    void testDeleteWizardSuccess() throws Exception {
        mockMvc.perform(
                        delete(baseUrl + "/wizards/3")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(true));
    }

    @Test
    @DisplayName("Check assignArtifact with valid ids (PUT)")
    void testAssignArtifactSuccess() throws Exception {
        mockMvc.perform(
                        put(baseUrl + "/wizards/2/artifacts/1250808601744904191")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Artifact Assignment Success"));
    }

}