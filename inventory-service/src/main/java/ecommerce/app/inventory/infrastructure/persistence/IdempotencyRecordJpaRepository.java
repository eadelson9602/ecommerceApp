package ecommerce.app.inventory.infrastructure.persistence;

import ecommerce.app.inventory.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecord, String> {
}
