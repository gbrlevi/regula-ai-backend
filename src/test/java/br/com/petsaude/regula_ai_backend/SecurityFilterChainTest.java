package br.com.petsaude.regula_ai_backend;

import br.com.petsaude.regula_ai_backend.Controller.PacienteController;
import br.com.petsaude.regula_ai_backend.Service.PacienteService;
import br.com.petsaude.regula_ai_backend.auth.EntryPointNaoAutenticado;
import br.com.petsaude.regula_ai_backend.auth.HandlerAcessoNegado;
import br.com.petsaude.regula_ai_backend.auth.TokenService;
import br.com.petsaude.regula_ai_backend.config.CorsConfig;
import br.com.petsaude.regula_ai_backend.config.JwtConfig;
import br.com.petsaude.regula_ai_backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica a cadeia de segurança sem depender de banco -- o que importa porque o
 * único outro teste do projeto ({@code RegulaAiBackendApplicationTests}) é um
 * {@code @SpringBootTest} que exige um Postgres real.
 */
@WebMvcTest(controllers = PacienteController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class, TokenService.class,
        EntryPointNaoAutenticado.class, HandlerAcessoNegado.class})
@TestPropertySource(properties = {
        "app.jwt.segredo=segredo-de-teste-com-mais-de-32-bytes-para-hs256",
        "app.jwt.emissor=regula-ai-backend",
        "app.jwt.expiracao-minutos=480",
        "app.cors.origens-permitidas=http://localhost:5173",
        "app.rate-limit.login.tentativas=5",
        "app.rate-limit.login.janela-minutos=15",
        "app.rate-limit.api.requisicoes=120",
        "app.rate-limit.api.janela-minutos=1",
        "springdoc.api-docs.enabled=false"
})
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private PacienteService pacienteService;

    /** O alvo aqui e a cadeia de filtros, nao o repositorio de contas. */
    @MockitoBean
    private UserDetailsService userDetailsService;

    private String tokenValido() {
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "regula", null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
        return tokenService.gerar(autenticacao).valor();
    }

    @Test
    void semTokenRetorna401ComCorpoJson() throws Exception {
        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void tokenAdulteradoRetorna401() throws Exception {
        String adulterado = tokenValido() + "x";
        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lixoNoHeaderRetorna401() throws Exception {
        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenEmitidoPeloProprioServicoEhAceito() throws Exception {
        when(pacienteService.listar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + tokenValido()))
                .andExpect(status().isOk());
    }

    @Test
    void documentacaoFicaFechadaQuandoSpringdocDesligado() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }
}
