package br.com.petsaude.regula_ai_backend.DTO;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "usuario e obrigatorio") String usuario,
        @NotBlank(message = "senha e obrigatoria") String senha
) {}
