package br.com.petsaude.regula_ai_backend.Repository;

import br.com.petsaude.regula_ai_backend.entity.ContaAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, UUID> {

    Optional<ContaAcesso> findByUsuarioAndAtivoTrue(String usuario);

    Optional<ContaAcesso> findByUsuario(String usuario);
}
