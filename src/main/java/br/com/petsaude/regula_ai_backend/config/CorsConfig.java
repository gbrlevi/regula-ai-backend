package br.com.petsaude.regula_ai_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS como um {@link CorsConfigurationSource}, e não mais como {@code WebMvcConfigurer}.
 *
 * <p>O motivo é a cadeia de filtros do Spring Security: ela precisa tratar o
 * preflight {@code OPTIONS} -- que chega sem {@code Authorization} -- antes da
 * autorização, e para isso consome este bean via {@code http.cors(...)}.
 *
 * <p>CORS não é controle de acesso: é uma política aplicada pelo navegador, que
 * {@code curl} e Postman ignoram. Quem protege a API é o {@link SecurityConfig}.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.origens-permitidas}")
    private String[] origensPermitidas;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Padroes (e nao origens exatas) para cobrir os preview deploys da Vercel.
        // Atencao: o header Origin nunca tem barra final -- um valor como
        // "https://exemplo.app/" nunca casa e derruba silenciosamente o CORS.
        config.setAllowedOriginPatterns(Arrays.stream(origensPermitidas)
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .toList());

        // POST e necessario para /api/auth/login; sem ele o preflight barra o login.
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // A autenticacao e por header Bearer, nao por cookie. Manter em false evita
        // a restricao que proibe curinga em origem quando credentials estao ligadas.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
