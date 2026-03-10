package ecommerce.app.inventory.application.port.in;

import ecommerce.app.inventory.domain.Inventory;

import java.util.UUID;

/**
 * Caso de uso: crear o actualizar inventario para un producto.
 */
public interface SetInventoryUseCase {

	Inventory setOrUpdateInventory(UUID productId, int available);
}
