package ecommerce.app.inventory.application.port.out;

import ecommerce.app.inventory.domain.IdempotencyRecord;

import java.util.Optional;

/**
 * Puerto de salida: almacén de respuestas idempotentes.
 */
public interface IdempotencyRecordPort {

	Optional<IdempotencyRecord> findByIdempotencyKey(String key);

	void save(IdempotencyRecord record);
}
