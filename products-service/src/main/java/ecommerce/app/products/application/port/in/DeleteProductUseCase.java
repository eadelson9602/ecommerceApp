package ecommerce.app.products.application.port.in;

import java.util.UUID;

/**
 * Caso de uso: eliminar producto (puerto de entrada).
 */
public interface DeleteProductUseCase {

	boolean deleteById(UUID id);
}
