package edu.tcu.cs.hogwartsartifactsonline.hogwartsuser;

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
import org.springframework.test.web.servlet.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration tests for User API endpoints")
@Tag("integration")
class HogwartsUserControllerIntegrationTest {

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
        JSONObject json = new JSONObject(response);

        this.token = "Bearer " + json.getJSONObject("data").getString("token");
    }

    @Test
    @DisplayName("Check findAllUsers (GET)")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testFindAllUsersSuccess() throws Exception {
        this.mockMvc.perform(
                        get(this.baseUrl + "/users")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find All Success"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    @DisplayName("Check findUserById (GET)")
    void testFindUserByIdSuccess() throws Exception {
        this.mockMvc.perform(
                        get(this.baseUrl + "/users/2")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find One Success"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.username").value("eric"));
    }

    @Test
    @DisplayName("Check findUserById with non-existent id (GET)")
    void testFindUserByIdNotFound() throws Exception {
        this.mockMvc.perform(
                        get(this.baseUrl + "/users/5")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND))
                .andExpect(jsonPath("$.message").value("Could not find user with Id 5 :("))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Check addUser with valid input (POST)")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testAddUserSuccess() throws Exception {
        HogwartsUser user = new HogwartsUser();
        user.setUsername("lily");
        user.setPassword("123456");
        user.setEnabled(true);
        user.setRoles("admin user");

        String json = this.objectMapper.writeValueAsString(user);

        this.mockMvc.perform(
                        post(this.baseUrl + "/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add Success"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("lily"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.roles").value("admin user"));

        this.mockMvc.perform(
                        get(this.baseUrl + "/users")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.data", Matchers.hasSize(4)));
    }

    @Test
    @DisplayName("Check addUser with invalid input (POST)")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testAddUserErrorWithInvalidInput() throws Exception {
        HogwartsUser user = new HogwartsUser();
        user.setUsername("");
        user.setPassword("");
        user.setRoles("");

        String json = this.objectMapper.writeValueAsString(user);

        this.mockMvc.perform(
                        post(this.baseUrl + "/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("Provided arguments are invalid, see data for details."))
                .andExpect(jsonPath("$.data.username").value("username is required."))
                .andExpect(jsonPath("$.data.password").value("password is required."))
                .andExpect(jsonPath("$.data.roles").value("roles are required."));
    }

    @Test
    @DisplayName("Check updateUser with valid input (PUT)")
    void testUpdateUserSuccess() throws Exception {
        HogwartsUser user = new HogwartsUser();
        user.setUsername("tom123");
        user.setEnabled(false);
        user.setRoles("user");

        String json = this.objectMapper.writeValueAsString(user);

        this.mockMvc.perform(
                        put(this.baseUrl + "/users/3")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.data.username").value("tom123"));
    }

    @Test
    @DisplayName("Check updateUser with non-existent id (PUT)")
    void testUpdateUserErrorWithNonExistentId() throws Exception {
        HogwartsUser user = new HogwartsUser();
        user.setId(5);
        user.setUsername("john123");
        user.setEnabled(true);
        user.setRoles("admin user");

        String json = this.objectMapper.writeValueAsString(user);

        this.mockMvc.perform(
                        put(this.baseUrl + "/users/5")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND));
    }

    @Test
    @DisplayName("Check deleteUser with valid input (DELETE)")
    void testDeleteUserSuccess() throws Exception {
        this.mockMvc.perform(
                        delete(this.baseUrl + "/users/2")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, this.token)
                )
                .andExpect(jsonPath("$.flag").value(true));
    }

}