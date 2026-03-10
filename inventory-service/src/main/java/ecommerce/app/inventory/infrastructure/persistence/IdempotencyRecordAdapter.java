package ecommerce.app.inventory.infrastructure.persistence;

import ecommerce.app.inventory.application.port.out.IdempotencyRecordPort;
import ecommerce.app.inventory.domain.IdempotencyRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdempotencyRecordAdapter implements IdempotencyRecordPort {

	private final IdempotencyRecordJpaRepository jpaRepository;

	public IdempotencyRecordAdapter(IdempotencyRecordJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<IdempotencyRecord> findByIdempotencyKey(String key) {
		return jpaRepository.findById(key);
	}

	@Override
	public void save(IdempotencyRecord record) {
		jpaRepository.save(record);
	}
}
