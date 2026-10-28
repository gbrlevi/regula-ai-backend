package br.com.petsaude.regula_ai_backend;

import br.com.petsaude.regula_ai_backend.Service.ImportacaoTransvaginalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportacaoTransvaginalRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoTransvaginalRunner.class);
    private static final int CHUNK_SIZE = 500;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Uso: <caminho-do-csv>");
            System.exit(1);
        }

        Path arquivo = Path.of(args[0]);
        if (!Files.exists(arquivo)) {
            System.err.println("Arquivo não encontrado: " + arquivo.toAbsolutePath());
            System.exit(1);
        }

        SpringApplication app = new SpringApplication(RegulaAiBackendApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext ctx = app.run()) {
            executar(ctx.getBean(ImportacaoTransvaginalService.class), arquivo);
        }
    }

    private static void executar(ImportacaoTransvaginalService service, Path arquivo) throws Exception {
        log.info("=== IMPORTAÇÃO DA TABELA TRANSVAGINAL ===");
        log.info("Arquivo: {}", arquivo.toAbsolutePath());
        long inicio = System.currentTimeMillis();

        Set<String> existentes = service.codConsultasExistentes();
        log.info("[1/3] Registros que já existem na tabela: {}", existentes.size());

        List<Map<String, String>> linhas = service.ler(arquivo);
        log.info("[2/3] Linhas de dados no CSV: {}", linhas.size());

        int totalChunks = (linhas.size() + CHUNK_SIZE - 1) / CHUNK_SIZE;
        log.info("[3/3] Processando em {} blocos de {} linhas...", totalChunks, CHUNK_SIZE);

        int criados = 0, atualizados = 0, totalErros = 0;
        List<String> erros = new ArrayList<>();

        for (int i = 0; i < linhas.size(); i += CHUNK_SIZE) {
            int fim = Math.min(i + CHUNK_SIZE, linhas.size());
            ImportacaoTransvaginalService.Resultado r =
                    service.processarChunk(linhas.subList(i, fim), existentes);

            criados += r.criados();
            atualizados += r.atualizados();
            totalErros += r.erros().size();
            if (erros.size() < 20) erros.addAll(r.erros());

            log.info("  Bloco {}/{} | linhas {}-{} | +{} criados | ~{} atualizados | {} erros",
                    (i / CHUNK_SIZE) + 1, totalChunks, i + 1, fim,
                    r.criados(), r.atualizados(), r.erros().size());
        }

        log.info("=== CONCLUÍDO ===");
        log.info("Tempo total: {}s | Linhas: {} | Criados: {} | Atualizados: {} | Erros: {}",
                (System.currentTimeMillis() - inicio) / 1000, linhas.size(), criados, atualizados, totalErros);
        erros.stream().limit(20).forEach(e -> log.warn("  - {}", e));
    }
}