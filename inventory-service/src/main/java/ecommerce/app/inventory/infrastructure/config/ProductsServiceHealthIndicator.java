package ecommerce.app.inventory.infrastructure.config;

import ecommerce.app.inventory.application.port.out.ProductsServicePort;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!test")
public class ProductsServiceHealthIndicator implements HealthIndicator {

	private final ProductsServicePort productsServicePort;

	public ProductsServiceHealthIndicator(ProductsServicePort productsServicePort) {
		this.productsServicePort = productsServicePort;
	}

	@Override
	public Health health() {
		try {
			UUID dummy = UUID.fromString("00000000-0000-0000-0000-000000000001");
			productsServicePort.getProductExists(dummy).block();
			return Health.up().withDetail("productsService", "reachable").build();
		} catch (Exception e) {
			return Health.down().withDetail("productsService", "unreachable")
					.withDetail("error", e.getMessage()).build();
		}
	}
}
