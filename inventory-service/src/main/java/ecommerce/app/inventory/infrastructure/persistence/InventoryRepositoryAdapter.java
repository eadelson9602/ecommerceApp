package ecommerce.app.inventory.infrastructure.persistence;

import ecommerce.app.inventory.application.port.out.InventoryRepositoryPort;
import ecommerce.app.inventory.domain.Inventory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryRepositoryAdapter implements InventoryRepositoryPort {

	private final InventoryJpaRepository jpaRepository;

	public InventoryRepositoryAdapter(InventoryJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<Inventory> findById(UUID productId) {
		return jpaRepository.findById(productId);
	}

	@Override
	public Optional<Inventory> findByProductIdForUpdate(UUID productId) {
		return jpaRepository.findByProductIdForUpdate(productId);
	}

	@Override
	public Inventory save(Inventory inventory) {
		return jpaRepository.save(inventory);
	}

	@Override
	public Inventory saveAndFlush(Inventory inventory) {
		return jpaRepository.saveAndFlush(inventory);
	}
}
