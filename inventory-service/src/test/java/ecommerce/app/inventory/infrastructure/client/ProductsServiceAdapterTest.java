package ecommerce.app.inventory.infrastructure.client;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del adaptador y excepciones del paquete client (cobertura).
 * El adaptador se ejercita en integración con @MockBean ProductsServicePort;
 * aquí se cubre el fallback y la excepción.
 */
class ProductsServiceAdapterTest {

	@Test
	void getProductExistsFallback_returnsErrorMono() {
		UUID productId = UUID.randomUUID();
		Exception ex = new RuntimeException("cause");
		ProductsServiceAdapter adapter = new ProductsServiceAdapter(null); // fallback no usa client

		assertThatThrownBy(() -> adapter.getProductExistsFallback(productId, ex).block())
				.isInstanceOf(ProductsServiceUnavailableException.class)
				.hasMessageContaining("unavailable");
	}

	@Test
	void productsServiceUnavailableException_hasMessageAndCause() {
		Throwable cause = new RuntimeException("cause");
		ProductsServiceUnavailableException ex =
				new ProductsServiceUnavailableException("Products service unavailable", cause);

		assertThatThrownBy(() -> { throw ex; })
				.isInstanceOf(ProductsServiceUnavailableException.class)
				.hasMessage("Products service unavailable");
		org.junit.jupiter.api.Assertions.assertSame(cause, ex.getCause());
	}
}
