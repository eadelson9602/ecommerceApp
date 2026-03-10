package ecommerce.app.inventory.application.port.out;

import ecommerce.app.inventory.domain.Inventory;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: persistencia de inventario.
 */
public interface InventoryRepositoryPort {

	Optional<Inventory> findById(UUID productId);

	Optional<Inventory> findByProductIdForUpdate(UUID productId);

	Inventory save(Inventory inventory);

	Inventory saveAndFlush(Inventory inventory);
}
