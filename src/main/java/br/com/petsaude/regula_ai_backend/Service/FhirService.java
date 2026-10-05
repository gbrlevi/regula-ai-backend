package br.com.petsaude.regula_ai_backend.Service;

import br.com.petsaude.regula_ai_backend.Repository.EncaminhamentoRepository;
import br.com.petsaude.regula_ai_backend.Repository.PacienteRepository;
import br.com.petsaude.regula_ai_backend.exception.RecursoNaoEncontradoException;
import ca.uhn.fhir.context.FhirContext;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FhirService {

    private final PacienteRepository pacienteRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final FhirConversorService conversor;
    private final FhirValidacaoService validacaoService;
    private final FhirContext fhirContext;

    // paciente

    public Patient buscarPatient(UUID id) {
        var paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado: " + id));
        return conversor.toPatient(paciente);
    }

    public String patientComoJson(UUID id) {
        return toJson(buscarPatient(id));
    }

    public FhirValidacaoService.ResultadoValidacao validarPatient(UUID id) {
        return validacaoService.validarRecurso(buscarPatient(id));
    }

    // encaminhamento

    public ServiceRequest buscarServiceRequest(UUID id) {
        var encaminhamento = encaminhamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encaminhamento não encontrado: id=" + id));
        return conversor.toServiceRequest(encaminhamento);
    }

    public String serviceRequestComoJson(UUID id) {
        return toJson(buscarServiceRequest(id));
    }

    public String serviceRequestPorCodComoJson(String codConsulta) {
        var encaminhamento = encaminhamentoRepository.findByCodConsulta(codConsulta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encaminhamento não encontrado: cod=" + codConsulta));
        return toJson(conversor.toServiceRequest(encaminhamento));
    }

    public FhirValidacaoService.ResultadoValidacao validarServiceRequest(UUID id) {
        return validacaoService.validarRecurso(buscarServiceRequest(id));
    }

    //auxiliares

    private String toJson(IBaseResource recurso) {
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(recurso);
    }
}