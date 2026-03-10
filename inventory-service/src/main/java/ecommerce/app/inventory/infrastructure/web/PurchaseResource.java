package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.app.inventory.domain.Purchase;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseResource {
	public static final String TYPE = "purchases";

	private String type = TYPE;
	private UUID id;
	private Map<String, Object> attributes;

	public static PurchaseResource from(Purchase p) {
		PurchaseResource r = new PurchaseResource();
		r.setType(TYPE);
		r.setId(p.getId());
		r.setAttributes(Map.of(
				"productId", p.getProductId().toString(),
				"quantity", p.getQuantity(),
				"processedAt", p.getProcessedAt().toString()
		));
		return r;
	}

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public Map<String, Object> getAttributes() { return attributes; }
	public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
