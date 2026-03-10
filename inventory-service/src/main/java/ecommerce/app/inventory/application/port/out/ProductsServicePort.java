package ecommerce.app.inventory.application.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Puerto de salida: verificación de existencia de producto en el catálogo externo.
 */
public interface ProductsServicePort {

	/**
	 * Comprueba si el producto existe. Retorna Mono con el UUID si existe, Mono.empty() si no.
	 */
	Mono<UUID> getProductExists(UUID productId);
}
