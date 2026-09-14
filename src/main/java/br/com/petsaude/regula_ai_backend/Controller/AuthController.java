package br.com.petsaude.regula_ai_backend.Controller;

import br.com.petsaude.regula_ai_backend.DTO.ErroResponseDTO;
import br.com.petsaude.regula_ai_backend.DTO.LoginRequestDTO;
import br.com.petsaude.regula_ai_backend.DTO.LoginResponseDTO;
import br.com.petsaude.regula_ai_backend.auth.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Fica no pacote {@code Controller} de propósito: o {@code GlobalExceptionHandler}
 * é um {@code @RestControllerAdvice} restrito a {@code basePackages = "...Controller"}.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Emissao de token de acesso")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica e devolve um token Bearer")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO requisicao,
                                   HttpServletRequest http) {
        try {
            Authentication autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requisicao.usuario(), requisicao.senha()));

            TokenService.Token token = tokenService.gerar(autenticacao);
            log.info("Login bem-sucedido para '{}' de {}", requisicao.usuario(), http.getRemoteAddr());

            return ResponseEntity.ok(
                    LoginResponseDTO.bearer(token.valor(), token.expiraEmSegundos()));

        } catch (AuthenticationException ex) {
            // Sinal de forca bruta: e o unico vetor relevante contra uma senha compartilhada.
            log.warn("Falha de login para '{}' de {}", requisicao.usuario(), http.getRemoteAddr());

            // Resposta unica para usuario inexistente e senha errada, para nao permitir enumeracao.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErroResponseDTO(401, "Usuario ou senha invalidos",
                            LocalDateTime.now().toString()));
        }
    }
}
