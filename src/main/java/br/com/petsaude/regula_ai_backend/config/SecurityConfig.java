package br.com.petsaude.regula_ai_backend.config;

import br.com.petsaude.regula_ai_backend.auth.EntryPointNaoAutenticado;
import br.com.petsaude.regula_ai_backend.auth.HandlerAcessoNegado;
import br.com.petsaude.regula_ai_backend.auth.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] CAMINHOS_DOCS = {
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    private final EntryPointNaoAutenticado entryPointNaoAutenticado;
    private final HandlerAcessoNegado handlerAcessoNegado;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtDecoder jwtDecoder;

    @Value("${app.rate-limit.login.tentativas}")
    private int loginTentativas;

    @Value("${app.rate-limit.login.janela-minutos}")
    private long loginJanelaMinutos;

    @Value("${app.rate-limit.api.requisicoes}")
    private int apiRequisicoes;

    @Value("${app.rate-limit.api.janela-minutos}")
    private long apiJanelaMinutos;

    /** Só libera a documentação onde o springdoc está ligado -- em produção ela fica fechada. */
    @Value("${springdoc.api-docs.enabled:true}")
    private boolean docsHabilitadas;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Construído explicitamente como {@link ProviderManager}, e <b>não</b> derivado de
     * {@code AuthenticationConfiguration.getAuthenticationManager()}.
     *
     * <p>Aquele atalho produz um delegador que resolve justamente o bean
     * {@code AuthenticationManager} -- ou seja, ele mesmo. A recursão só aparece no
     * caminho de token inválido, quando o {@code ProviderManager} do resource server
     * não consegue autenticar e recorre ao seu parent: {@code StackOverflowError}
     * em vez de 401.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Sem cookie de sessao e sem autenticacao por cookie, nao ha vetor de CSRF:
                // o token viaja no header Authorization, que um site terceiro nao consegue forjar.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                    if (docsHabilitadas) {
                        auth.requestMatchers(CAMINHOS_DOCS).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        // Gerenciador dedicado: isola a validacao do JWT do gerenciador de
                        // login, para que um token invalido nao caia no DaoAuthenticationProvider.
                        .authenticationManagerResolver(requisicao -> gerenciadorJwt())
                        .authenticationEntryPoint(entryPointNaoAutenticado)
                        .accessDeniedHandler(handlerAcessoNegado))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPointNaoAutenticado)
                        .accessDeniedHandler(handlerAcessoNegado))
                // Antes da autenticacao, para que tentativas de login que falham
                // tambem consumam o balde.
                .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationManager gerenciadorJwt() {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
        return new ProviderManager(provider);
    }

    /**
     * Instanciado direto (e não como {@code @Component}) para que o Boot não o
     * registre também como filtro de servlet, o que o faria rodar duas vezes.
     */
    private RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(
                loginTentativas, Duration.ofMinutes(loginJanelaMinutos),
                apiRequisicoes, Duration.ofMinutes(apiJanelaMinutos));
    }
}
