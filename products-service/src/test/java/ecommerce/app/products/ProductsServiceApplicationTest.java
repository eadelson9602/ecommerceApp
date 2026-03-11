package ecommerce.app.products;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifica que el contexto de Spring arranca correctamente
 * (paquete ecommerce.app.products, cobertura de aplicación).
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductsServiceApplicationTest {

	@Test
	void contextLoads() {
		// Si el contexto carga, el test pasa
	}
}
