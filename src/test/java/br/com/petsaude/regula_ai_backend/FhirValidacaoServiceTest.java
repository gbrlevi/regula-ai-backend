package br.com.petsaude.regula_ai_backend;

import br.com.petsaude.regula_ai_backend.Service.FhirValidacaoService;
import br.com.petsaude.regula_ai_backend.config.FhirConfig;
import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FhirValidacaoServiceTest {

    private FhirValidacaoService service;

    @BeforeEach
    void setup() {
        FhirContext ctx = FhirContext.forR4();
        service = new FhirValidacaoService(ctx, new FhirConfig().fhirValidator(ctx));
    }

    @Test
    void patientValidoDeveSerAceito() {
        String json = """
            {"resourceType":"Patient","gender":"female","birthDate":"1990-05-10"}
            """;
        var r = service.validarJson(json);
        assertTrue(r.valido(), r.mensagens().toString());
    }

    @Test
    void patientComGeneroInvalidoDeveFalhar() {
        String json = """
            {"resourceType":"Patient","gender":"banana"}
            """;
        var r = service.validarJson(json);
        System.out.println(r.mensagens());
        assertFalse(r.valido());
    }
}