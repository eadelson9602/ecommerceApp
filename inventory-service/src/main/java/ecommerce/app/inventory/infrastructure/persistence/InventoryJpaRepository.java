package ecommerce.app.inventory.infrastructure.persistence;

import ecommerce.app.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface InventoryJpaRepository extends JpaRepository<Inventory, UUID> {

	@Lock(LockModeType.OPTIMISTIC)
	@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
	Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);
}
