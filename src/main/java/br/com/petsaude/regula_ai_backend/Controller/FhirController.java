package br.com.petsaude.regula_ai_backend.Controller;

import br.com.petsaude.regula_ai_backend.Service.FhirService;
import br.com.petsaude.regula_ai_backend.Service.FhirValidacaoService;
import br.com.petsaude.regula_ai_backend.Service.FhirValidacaoService.ResultadoValidacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fhir")
@RequiredArgsConstructor
@Tag(name = "FHIR", description = "Exposição e validação de recursos FHIR (HL7 R4)")
public class FhirController {

    private final FhirService fhirService;
    private final FhirValidacaoService validacaoService;

    // paciente
    @GetMapping(value = "/pacientes/{id}", produces = "application/fhir+json")
    @Operation(summary = "Devolve o paciente como recurso FHIR Patient")
    public String paciente(@PathVariable UUID id) {
        return fhirService.patientComoJson(id);
    }

    @GetMapping("/pacientes/{id}/validacao")
    @Operation(summary = "Converte o paciente para FHIR e devolve o resultado da validação")
    public ResultadoValidacao validarPaciente(@PathVariable UUID id) {
        return fhirService.validarPatient(id);
    }

    // encaminhamento

    @GetMapping(value = "/encaminhamentos/{id}", produces = "application/fhir+json")
    @Operation(summary = "Devolve o encaminhamento como recurso FHIR ServiceRequest")
    public String encaminhamento(@PathVariable UUID id) {
        return fhirService.serviceRequestComoJson(id);
    }

    @GetMapping(value = "/encaminhamentos/cod/{codConsulta}", produces = "application/fhir+json")
    @Operation(summary = "Devolve o encaminhamento (por código de consulta) como ServiceRequest")
    public String encaminhamentoPorCod(@PathVariable String codConsulta) {
        return fhirService.serviceRequestPorCodComoJson(codConsulta);
    }

    @GetMapping("/encaminhamentos/{id}/validacao")
    @Operation(summary = "Converte o encaminhamento para FHIR e devolve o resultado da validação")
    public ResultadoValidacao validarEncaminhamento(@PathVariable UUID id) {
        return fhirService.validarServiceRequest(id);
    }

    // recebimento

    @PostMapping(value = "/validar", consumes = {"application/json", "application/fhir+json"})
    @Operation(summary = "Valida um recurso FHIR recebido (JSON R4)")
    public ResultadoValidacao validar(@RequestBody String json) {
        return validacaoService.validarJson(json);
    }
}