package br.com.petsaude.regula_ai_backend.Service;

import br.com.petsaude.regula_ai_backend.entity.Diagnostico;
import br.com.petsaude.regula_ai_backend.entity.Encaminhamento;
import br.com.petsaude.regula_ai_backend.entity.Paciente;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

@Service
public class FhirConversorService {

    private static final String SYS_COD_USUARIO = "urn:petsaude:cod-usuario";
    private static final String SYS_PRONTUARIO = "urn:petsaude:numero-prontuario";
    private static final String SYS_COD_CONSULTA = "urn:petsaude:cod-consulta";
    private static final String SYS_CID10 = "http://hl7.org/fhir/sid/icd-10";

    public Patient toPatient(Paciente p) {
        Patient fhir = new Patient();

        if (p.getId() != null) {
            fhir.setId(p.getId().toString());
        }

        fhir.addIdentifier().setSystem(SYS_COD_USUARIO).setValue(p.getCodUsuario());
        if (p.getNumeroProntuario() != null && !p.getNumeroProntuario().isBlank()) {
            fhir.addIdentifier().setSystem(SYS_PRONTUARIO).setValue(p.getNumeroProntuario());
        }

        fhir.setGender(mapSexo(p.getSexo()));

        if (p.getNascimento() != null) {
            fhir.setBirthDateElement(new DateType(p.getNascimento().toString()));
        }

        boolean temEndereco = (p.getMunicipio() != null && !p.getMunicipio().isBlank())
                || (p.getBairro() != null && !p.getBairro().isBlank());
        if (temEndereco) {
            Address a = fhir.addAddress();
            if (p.getMunicipio() != null && !p.getMunicipio().isBlank()) {
                a.setCity(p.getMunicipio());
            }
            if (p.getBairro() != null && !p.getBairro().isBlank()) {
                a.addLine(p.getBairro());
            }
        }
        return fhir;
    }

    public ServiceRequest toServiceRequest(Encaminhamento e) {
        ServiceRequest sr = new ServiceRequest();

        if (e.getId() != null) {
            sr.setId(e.getId().toString());
        }

        sr.addIdentifier().setSystem(SYS_COD_CONSULTA).setValue(e.getCodConsulta());
        sr.setStatus(mapStatus(e.getSituacao()));
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        sr.setPriority(mapPrioridade(e.getPrioridade()));

        if (e.getPaciente() != null && e.getPaciente().getId() != null) {
            sr.setSubject(new Reference("Patient/" + e.getPaciente().getId()));
        }

        if (e.getDtCadastro() != null) {
            sr.setAuthoredOn(Date.from(e.getDtCadastro().atZone(ZoneId.systemDefault()).toInstant()));
        }

        if (e.getMotivoEncaminhamento() != null && !e.getMotivoEncaminhamento().isBlank()) {
            sr.addReasonCode().setText(e.getMotivoEncaminhamento());
        }

        Diagnostico cid = e.getCidPrincipal();
        if (cid != null && cid.getCidCodigo() != null) {
            sr.addReasonCode().addCoding()
                    .setSystem(SYS_CID10)
                    .setCode(cid.getCidCodigo())
                    .setDisplay(cid.getDescricao());
        }

        return sr;
    }

    // TODO: Mapear com os valores reais de "sexo" do banco
    private AdministrativeGender mapSexo(String sexo) {
        if (sexo == null) return AdministrativeGender.UNKNOWN;
        return switch (sexo.trim().toUpperCase(Locale.ROOT)) {
            case "M", "MASCULINO" -> AdministrativeGender.MALE;
            case "F", "FEMININO" -> AdministrativeGender.FEMALE;
            default -> AdministrativeGender.UNKNOWN;
        };
    }

    // TODO: tudo provisório, depende dos valores reais de "situacao"
    private ServiceRequest.ServiceRequestStatus mapStatus(String situacao) {
        return ServiceRequest.ServiceRequestStatus.ACTIVE;
    }

    // TODO: tudo provisório, depende dos valores reais de "prioridade"
    private ServiceRequest.ServiceRequestPriority mapPrioridade(String prioridade) {
        return ServiceRequest.ServiceRequestPriority.ROUTINE;
    }
}