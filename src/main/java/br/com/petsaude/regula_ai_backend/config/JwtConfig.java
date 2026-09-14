package br.com.petsaude.regula_ai_backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Emissão e validação de JWT com segredo simétrico (HS256).
 *
 * <p>HS256 e não RS256 porque emissor e validador são o mesmo processo: um par de
 * chaves só se paga quando existem validadores independentes de quem emite.
 *
 * <p>A codificação/decodificação fica a cargo do Nimbus via Spring Security, e não
 * de um filtro artesanal: validação de assinatura, {@code exp} e {@code iss} vêm
 * prontas, que é justamente onde se concentram os erros clássicos de JWT.
 */
@Configuration
public class JwtConfig {

    /** HS256 exige chave de pelo menos 256 bits; abaixo disso o segredo é quebrável offline. */
    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    private static final String ALGORITMO = "HmacSHA256";

    private final SecretKeySpec chave;
    private final String emissor;

    public JwtConfig(@Value("${app.jwt.segredo}") String segredo,
                     @Value("${app.jwt.emissor}") String emissor) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.segredo tem " + bytes.length + " bytes; sao necessarios ao menos "
                    + TAMANHO_MINIMO_SEGREDO_BYTES + ". Gere um com: openssl rand -base64 48");
        }
        this.chave = new SecretKeySpec(bytes, ALGORITMO);
        this.emissor = emissor;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Valida exp/nbf (default) e tambem o emissor, para que um token assinado
        // com o mesmo segredo por outro sistema nao seja aceito aqui.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtIssuerValidator(emissor)));
        return decoder;
    }

    /**
     * Converte a claim {@code papeis} em authorities. Prefixo vazio porque os papéis
     * já são gravados com {@code ROLE_} na tabela.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("papeis");
        autoridades.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autoridades);
        return converter;
    }
}
