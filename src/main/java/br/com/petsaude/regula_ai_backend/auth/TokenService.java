package br.com.petsaude.regula_ai_backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Emite o access token a partir de uma autenticação já validada. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final String emissor;
    private final Duration expiracao;

    public TokenService(JwtEncoder encoder,
                        @Value("${app.jwt.emissor}") String emissor,
                        @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.encoder = encoder;
        this.emissor = emissor;
        this.expiracao = Duration.ofMinutes(expiracaoMinutos);
    }

    public Token gerar(Authentication authentication) {
        Instant agora = Instant.now();
        List<String> papeis = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .subject(authentication.getName())
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao))
                .claim("papeis", papeis)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new Token(valor, expiracao.toSeconds());
    }

    public record Token(String valor, long expiraEmSegundos) {}
}
