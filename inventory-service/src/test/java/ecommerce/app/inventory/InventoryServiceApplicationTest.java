package ecommerce.app.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifica que el contexto de Spring arranca correctamente.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceApplicationTest {

	@Test
	void contextLoads() {
		// Si el contexto carga, el test pasa
	}
}
