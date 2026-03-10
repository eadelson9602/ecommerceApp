package ecommerce.app.products.application.port.in;

import ecommerce.app.products.domain.Product;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: obtener un producto por id (puerto de entrada).
 */
public interface GetProductUseCase {

	Optional<Product> getById(UUID id);
}
