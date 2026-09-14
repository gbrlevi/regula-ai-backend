package br.com.petsaude.regula_ai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Conta usada para autenticar na API.
 *
 * <p>Não confundir com {@link Usuario}, que apesar do nome mapeia a tabela
 * {@code profissionais} e representa profissionais de saúde importados do CSV,
 * sem qualquer credencial.
 *
 * <p>{@code papeis} é uma lista separada por vírgula ({@code ROLE_USUARIO,ROLE_ADMIN}).
 * É o suficiente enquanto há uma conta compartilhada; quando o RBAC chegar, vira
 * uma tabela própria sem alterar o restante da cadeia de autenticação.
 */
@Entity
@Table(name = "contas_acesso", indexes = {
        @Index(name = "idx_contas_acesso_usuario", columnList = "usuario")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContaAcesso {

    public static final String PAPEL_PADRAO = "ROLE_USUARIO";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario", nullable = false, unique = true, length = 100)
    private String usuario;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "papeis", nullable = false, length = 255)
    @Builder.Default
    private String papeis = PAPEL_PADRAO;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Papéis como lista, ignorando entradas vazias. */
    public List<String> papeisComoLista() {
        if (papeis == null || papeis.isBlank()) {
            return List.of(PAPEL_PADRAO);
        }
        return Arrays.stream(papeis.split(","))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();
    }

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
