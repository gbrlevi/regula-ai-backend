package br.com.petsaude.regula_ai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Fixado no profile `test` (H2 em memória).
 *
 * <p>Sem isso o teste cai no profile `dev`, que faz {@code spring.config.import}
 * do `.env` -- hoje apontando para o Postgres de produção -- com
 * {@code ddl-auto: update}. Subir o contexto ali não é só leitura: o Hibernate
 * altera o schema e o {@code ContaAcessoBootstrap} semearia a conta de
 * desenvolvimento no banco real.
 */
@SpringBootTest
@ActiveProfiles("test")
class RegulaAiBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
