package ecommerce.app.inventory.infrastructure.client;

import ecommerce.app.inventory.application.port.out.ProductsServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adaptador que implementa el puerto de salida ProductsServicePort,
 * con Resilience4j (retry + circuit breaker).
 */
@Component
public class ProductsServiceAdapter implements ProductsServicePort {

	private final ProductsServiceClient client;

	public ProductsServiceAdapter(ProductsServiceClient client) {
		this.client = client;
	}

	@Override
	@Retry(name = "products")
	@CircuitBreaker(name = "products", fallbackMethod = "getProductExistsFallback")
	public Mono<UUID> getProductExists(UUID productId) {
		return client.getProductExists(productId);
	}

	public Mono<UUID> getProductExistsFallback(UUID productId, Exception ex) {
		return Mono.error(new ProductsServiceUnavailableException("Products service unavailable or timeout", ex));
	}
}
