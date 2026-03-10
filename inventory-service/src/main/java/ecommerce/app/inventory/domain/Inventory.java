package ecommerce.app.inventory.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inventory")
public class Inventory {

	@Id
	@Column(name = "product_id", columnDefinition = "uuid")
	private UUID productId;

	@Column(nullable = false)
	private int available;

	@Column(nullable = false)
	private int reserved;

	@Version
	private long version;

	public UUID getProductId() { return productId; }
	public void setProductId(UUID productId) { this.productId = productId; }
	public int getAvailable() { return available; }
	public void setAvailable(int available) { this.available = available; }
	public int getReserved() { return reserved; }
	public void setReserved(int reserved) { this.reserved = reserved; }
	public long getVersion() { return version; }
	public void setVersion(long version) { this.version = version; }
}
