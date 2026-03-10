package ecommerce.app.products.application.port.in;

import ecommerce.app.products.application.model.UpdateProductCommand;
import ecommerce.app.products.domain.Product;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: actualizar producto (puerto de entrada).
 */
public interface UpdateProductUseCase {

	Optional<Product> update(UUID id, UpdateProductCommand command);
}
