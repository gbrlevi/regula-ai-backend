package br.com.petsaude.regula_ai_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo contra H2: bootstrap da conta, login, e uso do token num endpoint
 * de dado sensível.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limit.login.tentativas=50")
class AutenticacaoIntegracaoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private String corpoLogin(String usuario, String senha) {
        return MAPPER.createObjectNode()
                .put("usuario", usuario)
                .put("senha", senha)
                .toString();
    }

    private String autenticar() throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("conta-de-teste", "senha-de-teste-longa")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(json).get("accessToken").asText();
    }

    @Test
    void contaDeBootstrapAutenticaEDevolveToken() throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("conta-de-teste", "senha-de-teste-longa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").value(480 * 60))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = MAPPER.readTree(json);
        assertThat(node.get("accessToken").asText()).isNotBlank();
    }

    @Test
    void senhaErradaRetorna401SemRevelarSeUsuarioExiste() throws Exception {
        String existente = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("conta-de-teste", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String inexistente = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("nao-existe", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Mesma resposta nos dois casos: caso contrario da para enumerar usuarios.
        assertThat(MAPPER.readTree(existente).get("mensagem").asText())
                .isEqualTo(MAPPER.readTree(inexistente).get("mensagem").asText());
    }

    @Test
    void endpointDePacientesExigeTokenEAceitaOEmitidoNoLogin() throws Exception {
        mockMvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + autenticar()))
                .andExpect(status().isOk());
    }

    @Test
    void corpoInvalidoRetorna400ENao500() throws Exception {
        // Campos em branco violam @NotBlank; sem handler dedicado isto cairia no
        // catch-all de Exception do GlobalExceptionHandler e viraria 500.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void tetoDePaginacaoLimitaOTamanhoPedido() throws Exception {
        mockMvc.perform(get("/api/pacientes").param("size", "5000")
                        .header("Authorization", "Bearer " + autenticar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(100));
    }
}
