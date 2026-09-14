package br.com.petsaude.regula_ai_backend.auth;

import br.com.petsaude.regula_ai_backend.Repository.ContaAcessoRepository;
import br.com.petsaude.regula_ai_backend.entity.ContaAcesso;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaAcessoUserDetailsService implements UserDetailsService {

    private final ContaAcessoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usuario) throws UsernameNotFoundException {
        ContaAcesso conta = repository.findByUsuarioAndAtivoTrue(usuario)
                // Mensagem propositalmente genérica: quem chama converte em 401 sem
                // distinguir "usuario inexistente" de "senha errada" (evita enumeracao).
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));

        List<SimpleGrantedAuthority> autoridades = conta.papeisComoLista().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return User.withUsername(conta.getUsuario())
                .password(conta.getSenhaHash())
                .authorities(autoridades)
                .build();
    }
}
