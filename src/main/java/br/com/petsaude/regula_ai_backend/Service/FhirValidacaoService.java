package br.com.petsaude.regula_ai_backend.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.springframework.stereotype.Service;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

@Service
public class FhirValidacaoService {

    private final FhirContext ctx;
    private final FhirValidator validator;

    public FhirValidacaoService(FhirContext ctx, FhirValidator validator) {
        this.ctx = ctx;
        this.validator = validator;
    }
    
    public ResultadoValidacao validarJson(String json) {
        ValidationResult result = validator.validateWithResult(json);
        return converter(result);
    }

    private ResultadoValidacao converter(ValidationResult result) {
        List<Mensagem> mensagens = result.getMessages().stream()
                .map(m -> new Mensagem(m.getSeverity().name(), m.getLocationString(), m.getMessage()))
                .toList();
        return new ResultadoValidacao(result.isSuccessful(), mensagens);
    }

    public ResultadoValidacao validarRecurso(IBaseResource recurso) {
    return converter(validator.validateWithResult(recurso));
    }

    //todo: tirar os record daqui

    public record Mensagem(String severidade, String local, String mensagem) {}
    public record ResultadoValidacao(boolean valido, List<Mensagem> mensagens) {}
}