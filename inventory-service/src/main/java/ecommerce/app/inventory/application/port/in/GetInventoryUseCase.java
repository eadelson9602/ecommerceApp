package ecommerce.app.inventory.application.port.in;

import ecommerce.app.inventory.application.model.GetInventoryResult;

import java.util.UUID;

/**
 * Caso de uso: consultar inventario validando que el producto exista en el catálogo.
 */
public interface GetInventoryUseCase {

	GetInventoryResult getInventoryValidatingProduct(UUID productId);
}
