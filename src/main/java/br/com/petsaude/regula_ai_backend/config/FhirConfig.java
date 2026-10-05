package br.com.petsaude.regula_ai_backend.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class FhirConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public FhirValidator fhirValidator(FhirContext ctx) {
        //onde o validador busca as definições
        ValidationSupportChain chain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(ctx),   // perfis base do FHIR R4
                new InMemoryTerminologyServerValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx)
        );

        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(chain);

        FhirValidator validator = ctx.newValidator();
        validator.registerValidatorModule(instanceValidator);
        return validator;
    }
}