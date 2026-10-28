package br.com.petsaude.regula_ai_backend.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportacaoTransvaginalService {

    private static final DateTimeFormatter[] FORMATOS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private final ImportacaoFilaService filaService;
    private final JdbcTemplate jdbc;

    private String sql;


    public Set<String> codConsultasExistentes() {
        return new HashSet<>(jdbc.queryForList(
                "SELECT cod_consulta FROM public.transvaginal", String.class));
    }

    public List<Map<String, String>> ler(Path arquivo) throws IOException {
        return filaService.parsearCsv(arquivo);
    }

    @Transactional
    public Resultado processarChunk(List<Map<String, String>> linhas, Set<String> existentes) {
        List<Object[]> lote = new ArrayList<>();
        List<String> codigos = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        for (Map<String, String> row : linhas) {
            String cod = texto(row, "Cod Consulta", 20);
            String usuario = texto(row, "Cód Usuário", 20);
            if (cod == null || usuario == null) {
                erros.add("Linha ignorada: Cod Consulta ou Cód Usuário ausente");
                continue;
            }
            lote.add(converter(row, UUID.randomUUID()).values().toArray());
            codigos.add(cod);
        }

        int criados = 0, atualizados = 0;
        if (!lote.isEmpty()) {
            int[] resultados = jdbc.batchUpdate(sql(), lote);
            for (int i = 0; i < resultados.length; i++) {
                String cod = codigos.get(i);
                if (!existentes.contains(cod)) {
                    criados++;
                } else if (resultados[i] != 0) {
                    atualizados++;
                }
                existentes.add(cod);
            }
        }
        return new Resultado(criados, atualizados, erros);
    }

    public record Resultado(int criados, int atualizados, List<String> erros) {}


    private String sql() {
        if (sql == null) {
            List<String> colunas = new ArrayList<>(converter(Map.of(), UUID.randomUUID()).keySet());
            String lista = String.join(", ", colunas);
            String marcadores = colunas.stream().map(c -> "?").collect(Collectors.joining(", "));
            String atualizacoes = colunas.stream()
                    .filter(c -> !c.equals("id") && !c.equals("cod_consulta"))
                    .map(c -> c + " = EXCLUDED." + c)
                    .collect(Collectors.joining(", "));
            sql = "INSERT INTO public.transvaginal (" + lista + ") VALUES (" + marcadores + ") "
                + "ON CONFLICT (cod_consulta) DO UPDATE SET " + atualizacoes + ", atualizado_em = NOW() "
                + "WHERE transvaginal.dt_ultima_alteracao IS NULL "
                + "OR EXCLUDED.dt_ultima_alteracao > transvaginal.dt_ultima_alteracao";
        }
        return sql;
    }

    private Map<String, Object> converter(Map<String, String> r, UUID id) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", id);
        v.put("cod_consulta", texto(r, "Cod Consulta", 20));
        v.put("cod_usuario", texto(r, "Cód Usuário", 20));
        v.put("numero_prontuario", texto(r, "Nº Prontuário", 30));
        v.put("posicao", inteiro(r, "Posição"));
        v.put("prioridade", texto(r, "Prioridade", 100));
        v.put("tipo", texto(r, "Tipo", 100));
        v.put("situacao", texto(r, "Situação", 200));
        v.put("perfil", texto(r, "Perfil", 100));
        v.put("origem_solicitacao", texto(r, "Origem da Solicitação", 200));
        v.put("sexo", texto(r, "Sexo", 10));
        v.put("nascimento", data(r, "Nascimento"));
        v.put("idade", inteiro(r, "Idade"));
        v.put("bairro", texto(r, "Bairro", 120));
        v.put("municipio", texto(r, "Município", 120));
        v.put("municipio_usuario", texto(r, "Município Usuário", 120));
        v.put("microarea", texto(r, "Microarea", 120));
        v.put("equipe", texto(r, "Equipe", 120));
        v.put("cid_10", texto(r, "CID- 10", 10));
        v.put("descricao_cid", texto(r, "Descrição CID", 500));
        v.put("cid_10_prioritario", booleano(r, "CID- 10 Prioritário"));
        v.put("qtde_solicitada", inteiro(r, "Qtde Solicitada"));
        v.put("dia_minimo_sessao", inteiro(r, "Dia Mínimo Sessão"));
        v.put("motivo_encaminhamento", texto(r, "Motivo do Encaminhamento", null));
        v.put("procedimento_principal", texto(r, "Procedimento Principal", 500));
        v.put("procedimento_sms", texto(r, "Procedimento SMS", 500));
        v.put("procedimentos_sus", texto(r, "Procedimento (s) SUS", 500));
        v.put("regulacao", booleano(r, "Regulação"));
        v.put("busca_ativa", booleano(r, "Busca Ativa"));
        v.put("processo_automatico", booleano(r, "Processo Automático"));
        v.put("solicitacao_leito", booleano(r, "Solicitação Leito"));
        v.put("dt_cadastro", dataHora(r, "Dt Cadastro"));
        v.put("dt_ultima_alteracao", dataHora(r, "Dt Última Alteração"));
        v.put("data_consulta", dataHora(r, "Data Consulta"));
        v.put("data_reservada", dataHora(r, "Data Reservada"));
        v.put("limite_confirmacao_reserva", dataHora(r, "Limite Confirmação Reserva"));
        v.put("dt_realizacao_agendamento", dataHora(r, "Dt Realização Agendamento"));
        v.put("dt_regulacao", dataHora(r, "Dt Regulação"));
        v.put("data_negativa", data(r, "Data da Negativa"));
        v.put("data_controle", data(r, "Data de Controle"));
        v.put("estabelecimento_solicitante", texto(r, "Estabelecimento Solicitante", 255));
        v.put("estabelecimento_sugerido", texto(r, "Estabelecimento Sugerido", 255));
        v.put("estabelecimento_prestador", texto(r, "Estabelecimento Prestador", 255));
        v.put("endereco_estabelecimento_prestador", texto(r, "Endereço do Estabelecimento Prestador", 500));
        v.put("profissional_sugerido", texto(r, "Profissional Sugerido", 255));
        v.put("profissional_solicitante", texto(r, "Profissional Solicitante", 255));
        v.put("regulador", texto(r, "Regulador", 255));
        v.put("cadastrado_por", texto(r, "Cadastrado por", 255));
        v.put("nome_responsavel", texto(r, "Nome Responsável", 255));
        v.put("nome_responsavel_busca_ativa", texto(r, "Nome Responsável Busca Ativa", 255));
        v.put("estab_responsavel_busca_ativa", texto(r, "Estab Responsável Busca Ativa", 255));
        v.put("motivo_negativa", texto(r, "Motivo da Negativa", 500));
        v.put("justif_negativa", texto(r, "Justif da Negativa", null));
        v.put("resp_negativa", texto(r, "Resp Negativa", 255));
        v.put("justif_cancelamento", texto(r, "Justif Cancelamento", null));
        return v;
    }

    private String texto(Map<String, String> r, String coluna, Integer max) {
        String v = r.get(normalizar(coluna));
        if (v == null || v.isBlank()) return null;
        v = v.trim();
        return (max != null && v.length() > max) ? v.substring(0, max) : v;
    }

    private Integer inteiro(Map<String, String> r, String coluna) {
        String v = texto(r, coluna, null);
        if (v == null) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean booleano(Map<String, String> r, String coluna) {
        String v = texto(r, coluna, null);
        if (v == null) return null;
        return switch (normalizar(v)) {
            case "sim", "s", "true" -> Boolean.TRUE;
            case "nao", "n", "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    private LocalDateTime dataHora(Map<String, String> r, String coluna) {
        String v = texto(r, coluna, null);
        if (v == null) return null;
        for (DateTimeFormatter fmt : FORMATOS) {
            try {
                return LocalDateTime.parse(v, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        try {
            return LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate data(Map<String, String> r, String coluna) {
        LocalDateTime dh = dataHora(r, coluna);
        return dh == null ? null : dh.toLocalDate();
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        String s = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replace("\uFEFF", "");
        return s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}