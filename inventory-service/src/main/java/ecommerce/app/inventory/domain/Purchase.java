package ecommerce.app.inventory.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase")
public class Purchase {

	@Id
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "idempotency_key", unique = true)
	private String idempotencyKey;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public UUID getProductId() { return productId; }
	public void setProductId(UUID productId) { this.productId = productId; }
	public int getQuantity() { return quantity; }
	public void setQuantity(int quantity) { this.quantity = quantity; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
	public Instant getProcessedAt() { return processedAt; }
	public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
