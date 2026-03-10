package ecommerce.app.inventory.infrastructure.persistence;

import ecommerce.app.inventory.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PurchaseJpaRepository extends JpaRepository<Purchase, UUID> {
}
