package br.com.petsaude.regula_ai_backend.auth;

import br.com.petsaude.regula_ai_backend.DTO.ErroResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Escreve {@link ErroResponseDTO} direto na resposta.
 *
 * <p>Necessário porque exceções lançadas na cadeia de filtros do Spring Security
 * acontecem antes do DispatcherServlet e nunca chegam ao {@code @RestControllerAdvice};
 * sem isto o frontend receberia HTML onde espera JSON.
 */
public final class RespostaErroJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RespostaErroJson() {}

    public static void escrever(HttpServletResponse response, int status, String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(),
                new ErroResponseDTO(status, mensagem, LocalDateTime.now().toString()));
    }
}
