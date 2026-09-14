package br.com.petsaude.regula_ai_backend.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita requisições por IP. Roda antes da autenticação, para que tentativas de
 * login sejam contadas mesmo quando falham.
 *
 * <p>Autenticação sozinha não resolve o problema: com uma conta compartilhada
 * sobram dois vetores, e ambos são de volume -- força bruta na senha e varredura
 * da base por quem já tem a credencial. O limite é o que transforma "vazou a senha"
 * em "exfiltrar a base leva semanas e aparece no log".
 *
 * <p>A chave é o IP, e não o usuário autenticado, porque com conta única
 * compartilhada todos os acessos são o mesmo {@code sub} -- chavear por usuário
 * seria um único balde global para toda a equipe.
 *
 * <p>O estado é local ao processo. Serve enquanto houver uma única instância
 * (o caso do plano free do Render); ao escalar horizontalmente, o limite passa a
 * ser por instância e exigirá um store compartilhado (ex.: Redis).
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String CAMINHO_LOGIN = "/api/auth/login";
    private static final String PREFIXO_API = "/api/";

    /** Acima disto, faz-se uma limpeza dos baldes ociosos antes de aceitar novos. */
    private static final int MAXIMO_CHAVES = 10_000;
    private static final Duration OCIOSIDADE_MAXIMA = Duration.ofMinutes(30);

    private final Map<String, Entrada> baldesLogin = new ConcurrentHashMap<>();
    private final Map<String, Entrada> baldesApi = new ConcurrentHashMap<>();

    private final int loginTentativas;
    private final Duration loginJanela;
    private final int apiRequisicoes;
    private final Duration apiJanela;

    public RateLimitFilter(int loginTentativas, Duration loginJanela,
                           int apiRequisicoes, Duration apiJanela) {
        this.loginTentativas = loginTentativas;
        this.loginJanela = loginJanela;
        this.apiRequisicoes = apiRequisicoes;
        this.apiJanela = apiJanela;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String caminho = request.getRequestURI();
        boolean ehLogin = CAMINHO_LOGIN.equals(caminho);

        if (!ehLogin && !caminho.startsWith(PREFIXO_API)) {
            chain.doFilter(request, response);
            return;
        }

        // Depende de server.forward-headers-strategy=framework em producao: atras do
        // proxy do Render, sem isso todo mundo compartilha o IP do proxy e o limite
        // de um cliente bane a equipe inteira.
        String ip = request.getRemoteAddr();

        Bucket balde = ehLogin
                ? obterBalde(baldesLogin, ip, loginTentativas, loginJanela, true)
                : obterBalde(baldesApi, ip, apiRequisicoes, apiJanela, false);

        ConsumptionProbe consumo = balde.tryConsumeAndReturnRemaining(1);

        if (consumo.isConsumed()) {
            chain.doFilter(request, response);

            // Login bem-sucedido devolve o token ao balde: o limite existe para
            // barrar forca bruta, e so falhas devem conta-lo. Sem isto, uma unidade
            // de saude atras de NAT esgotaria a cota com logins legitimos, ja que
            // toda a equipe compartilha o mesmo IP publico.
            if (ehLogin && response.getStatus() == HttpStatus.OK.value()) {
                balde.addTokens(1);
            }
            return;
        }

        long segundos = Math.max(1, consumo.getNanosToWaitForRefill() / 1_000_000_000L);
        log.warn("Rate limit excedido por {} em {} ({}s para liberar)", ip, caminho, segundos);

        response.setHeader("Retry-After", String.valueOf(segundos));
        RespostaErroJson.escrever(response, HttpStatus.TOO_MANY_REQUESTS.value(),
                "Muitas requisicoes. Tente novamente em " + segundos + " segundos.");
    }

    private Bucket obterBalde(Map<String, Entrada> baldes, String ip,
                              int capacidade, Duration janela, boolean intervalar) {
        if (baldes.size() > MAXIMO_CHAVES) {
            removerOciosos(baldes);
        }
        Entrada entrada = baldes.compute(ip, (chave, atual) -> {
            Entrada e = (atual != null) ? atual : new Entrada(criarBalde(capacidade, janela, intervalar));
            e.ultimoAcesso = System.nanoTime();
            return e;
        });
        return entrada.bucket;
    }

    private Bucket criarBalde(int capacidade, Duration janela, boolean intervalar) {
        // Login usa recarga intervalar (tudo de uma vez ao fim da janela), que e o
        // comportamento desejado para bloquear forca bruta; a API usa recarga
        // continua, que nao trava o uso legitimo em rajadas.
        Bandwidth limite = intervalar
                ? Bandwidth.builder().capacity(capacidade).refillIntervally(capacidade, janela).build()
                : Bandwidth.builder().capacity(capacidade).refillGreedy(capacidade, janela).build();
        return Bucket.builder().addLimit(limite).build();
    }

    private void removerOciosos(Map<String, Entrada> baldes) {
        long limite = System.nanoTime() - OCIOSIDADE_MAXIMA.toNanos();
        baldes.entrySet().removeIf(e -> e.getValue().ultimoAcesso < limite);
    }

    private static final class Entrada {
        private final Bucket bucket;
        private volatile long ultimoAcesso;

        private Entrada(Bucket bucket) {
            this.bucket = bucket;
            this.ultimoAcesso = System.nanoTime();
        }
    }
}
