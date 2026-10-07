package br.com.petsaude.regula_ai_backend;

import br.com.petsaude.regula_ai_backend.Service.FhirConversorService;
import br.com.petsaude.regula_ai_backend.Service.FhirValidacaoService;
import br.com.petsaude.regula_ai_backend.config.FhirConfig;
import br.com.petsaude.regula_ai_backend.entity.Diagnostico;
import br.com.petsaude.regula_ai_backend.entity.Encaminhamento;
import br.com.petsaude.regula_ai_backend.entity.Paciente;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FhirConversorServiceTest {

    private FhirContext ctx;
    private FhirConversorService conversor;
    private FhirValidacaoService validacao;

    @BeforeEach
    void setup() {
        ctx = FhirContext.forR4();
        conversor = new FhirConversorService();
        validacao = new FhirValidacaoService(ctx, new FhirConfig().fhirValidator(ctx));
    }

    @Test
    void pacienteConvertidoDeveSerFhirValido() {
        Paciente p = Paciente.builder()
                .id(UUID.randomUUID())
                .codUsuario("12345")
                .numeroProntuario("P-998")
                .sexo("F")
                .nascimento(LocalDate.of(1990, 5, 10))
                .municipio("Fortaleza")
                .bairro("Centro")
                .build();

        Patient fhir = conversor.toPatient(p);
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(fhir));

        var r = validacao.validarRecurso(fhir);
        assertTrue(r.valido(), r.mensagens().toString());
    }

    @Test
    void pacienteSoComCodUsuarioTambemDeveSerValido() {
        Paciente p = Paciente.builder().id(UUID.randomUUID()).codUsuario("999").build();

        var r = validacao.validarRecurso(conversor.toPatient(p));
        assertTrue(r.valido(), r.mensagens().toString());
    }

    @Test
    void encaminhamentoCompletoConvertidoDeveSerFhirValido() {
        Paciente p = Paciente.builder().id(UUID.randomUUID()).codUsuario("12345").build();
        Diagnostico cid = Diagnostico.builder()
                .cidCodigo("I10")
                .descricao("Hipertensão essencial")
                .build();
        Encaminhamento e = Encaminhamento.builder()
                .id(UUID.randomUUID())
                .codConsulta("C-0001")
                .paciente(p)
                .dtCadastro(LocalDateTime.of(2026, 9, 1, 10, 30))
                .prioridade("Alta")
                .situacao("Aguardando")
                .motivoEncaminhamento("Avaliação cardiológica")
                .cidPrincipal(cid)
                .build();

        ServiceRequest fhir = conversor.toServiceRequest(e);
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(fhir));

        var r = validacao.validarRecurso(fhir);
        System.out.println(r.mensagens());
        assertTrue(r.valido(), r.mensagens().toString());
    }

    @Test
    void encaminhamentoMinimoTambemDeveSerValido() {
        // o que a importação sempre preenche: código da consulta e paciente
        Paciente p = Paciente.builder().id(UUID.randomUUID()).codUsuario("999").build();
        Encaminhamento e = Encaminhamento.builder()
                .id(UUID.randomUUID())
                .codConsulta("C-0002")
                .paciente(p)
                .build();

        var r = validacao.validarRecurso(conversor.toServiceRequest(e));
        assertTrue(r.valido(), r.mensagens().toString());
    }
}