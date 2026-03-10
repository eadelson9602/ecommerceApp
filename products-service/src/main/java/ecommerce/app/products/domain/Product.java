package ecommerce.app.products.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		if (id == null) id = UUID.randomUUID();
		Instant now = Instant.now();
		if (createdAt == null) createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public String getSku() { return sku; }
	public void setSku(String sku) { this.sku = sku; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public BigDecimal getPrice() { return price; }
	public void setPrice(BigDecimal price) { this.price = price; }
	public ProductStatus getStatus() { return status; }
	public void setStatus(ProductStatus status) { this.status = status; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
