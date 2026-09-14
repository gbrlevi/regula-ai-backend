package br.com.petsaude.regula_ai_backend;

import br.com.petsaude.regula_ai_backend.Controller.PacienteController;
import br.com.petsaude.regula_ai_backend.Service.PacienteService;
import br.com.petsaude.regula_ai_backend.auth.EntryPointNaoAutenticado;
import br.com.petsaude.regula_ai_backend.auth.HandlerAcessoNegado;
import br.com.petsaude.regula_ai_backend.config.CorsConfig;
import br.com.petsaude.regula_ai_backend.config.JwtConfig;
import br.com.petsaude.regula_ai_backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O CORS antigo tinha três defeitos que só apareceriam em produção: barra final na
 * origem (que nunca casa com o header {@code Origin}), ausência de {@code POST}
 * (que barraria o preflight do login) e configuração via {@code WebMvcConfigurer},
 * que não governa a cadeia de filtros do Security.
 *
 * <p>A origem aqui é declarada <b>com</b> barra final de propósito, para provar que
 * a normalização funciona.
 */
@WebMvcTest(controllers = PacienteController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class,
        EntryPointNaoAutenticado.class, HandlerAcessoNegado.class})
@TestPropertySource(properties = {
        "app.jwt.segredo=segredo-de-teste-com-mais-de-32-bytes-para-hs256",
        "app.jwt.emissor=regula-ai-backend",
        "app.jwt.expiracao-minutos=480",
        "app.cors.origens-permitidas=https://regula-ai-front.vercel.app/",
        "app.rate-limit.login.tentativas=5",
        "app.rate-limit.login.janela-minutos=15",
        "app.rate-limit.api.requisicoes=120",
        "app.rate-limit.api.janela-minutos=1",
        "springdoc.api-docs.enabled=false"
})
class CorsPreflightTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteService pacienteService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void preflightDoLoginEhLiberadoMesmoComBarraFinalNaConfiguracao() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://regula-ai-front.vercel.app")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://regula-ai-front.vercel.app"));
    }

    @Test
    void preflightNaoPrecisaDeAutenticacao() throws Exception {
        // O preflight chega sem Authorization: se a autorizacao rodasse antes do
        // CORS, o navegador receberia 401 e nenhuma chamada sairia do frontend.
        mockMvc.perform(options("/api/pacientes")
                        .header("Origin", "https://regula-ai-front.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void origemDesconhecidaEhRejeitada() throws Exception {
        mockMvc.perform(get("/api/pacientes").header("Origin", "https://malicioso.example"))
                .andExpect(status().isForbidden());
    }
}
