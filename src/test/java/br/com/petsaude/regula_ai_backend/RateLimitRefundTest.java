package br.com.petsaude.regula_ai_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contexto próprio (propriedades diferentes de {@code RateLimitLoginTest}) porque o
 * balde vive no filtro singleton: compartilhar o contexto tornaria o resultado
 * dependente da ordem de execução dos testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.login.tentativas=2",
        "app.rate-limit.login.janela-minutos=15"
})
class RateLimitRefundTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginBemSucedidoNaoConsomeACota() throws Exception {
        String corpo = MAPPER.createObjectNode()
                .put("usuario", "conta-de-teste")
                .put("senha", "senha-de-teste-longa")
                .toString();

        // Seis logins corretos contra uma cota de duas tentativas. Se acertos
        // contassem, o terceiro seria 429 -- e uma unidade de saúde atrás de NAT,
        // onde toda a equipe compartilha o mesmo IP público, ficaria trancada.
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isOk());
        }
    }
}
