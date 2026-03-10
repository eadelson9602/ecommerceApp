package ecommerce.app.products.application.port.in;

import ecommerce.app.products.application.model.CreateProductCommand;
import ecommerce.app.products.domain.Product;

/**
 * Caso de uso: crear producto (puerto de entrada).
 */
public interface CreateProductUseCase {

	Product create(CreateProductCommand command);
}
