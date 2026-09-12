package br.com.petsaude.regula_ai_backend.auth;

import br.com.petsaude.regula_ai_backend.Repository.ContaAcessoRepository;
import br.com.petsaude.regula_ai_backend.entity.ContaAcesso;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria (ou atualiza a senha de) a conta inicial a partir de variáveis de ambiente.
 *
 * <p>A senha chega em texto claro pelo ambiente e só o hash BCrypt é persistido --
 * assim nenhum hash precisa ser gerado à mão nem versionado, e rotacionar a
 * credencial é trocar a variável e reiniciar a aplicação.
 *
 * <p>Se as propriedades estiverem vazias, nada acontece: é o caso de um ambiente
 * cuja conta já foi criada e cujas variáveis de bootstrap foram removidas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContaAcessoBootstrap implements ApplicationRunner {

    private final ContaAcessoRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.bootstrap.usuario:}")
    private String usuario;

    @Value("${app.auth.bootstrap.senha:}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            log.info("Bootstrap de conta de acesso ignorado: app.auth.bootstrap nao configurado.");
            return;
        }

        if (senha.length() < 12) {
            throw new IllegalStateException(
                    "app.auth.bootstrap.senha deve ter ao menos 12 caracteres. "
                    + "Esta e a unica credencial da API: use uma passphrase longa.");
        }

        repository.findByUsuario(usuario).ifPresentOrElse(
                conta -> {
                    // Reaplica o hash a cada boot para que trocar a variavel de ambiente
                    // seja suficiente para rotacionar a senha.
                    conta.setSenhaHash(passwordEncoder.encode(senha));
                    conta.setAtivo(true);
                    repository.save(conta);
                    log.info("Conta de acesso '{}' atualizada.", usuario);
                },
                () -> {
                    repository.save(ContaAcesso.builder()
                            .usuario(usuario)
                            .senhaHash(passwordEncoder.encode(senha))
                            .papeis(ContaAcesso.PAPEL_PADRAO)
                            .ativo(true)
                            .build());
                    log.info("Conta de acesso '{}' criada.", usuario);
                });
    }
}
