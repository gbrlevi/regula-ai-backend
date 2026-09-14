package br.com.petsaude.regula_ai_backend.DTO;

public record LoginResponseDTO(
        String tokenType,
        String accessToken,
        long expiraEm
) {
    public static LoginResponseDTO bearer(String accessToken, long expiraEmSegundos) {
        return new LoginResponseDTO("Bearer", accessToken, expiraEmSegundos);
    }
}
