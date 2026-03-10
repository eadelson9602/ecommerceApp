package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ecommerce.app.inventory.domain.Purchase;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseResource {
	public static final String TYPE = "purchases";

	@JsonProperty("type")
	private String resourceType = TYPE;
	private UUID id;
	private Map<String, Object> attributes;

	public static PurchaseResource from(Purchase p) {
		PurchaseResource r = new PurchaseResource();
		r.setResourceType(TYPE);
		r.setId(p.getId());
		r.setAttributes(Map.of(
				"productId", p.getProductId().toString(),
				"quantity", p.getQuantity(),
				"processedAt", p.getProcessedAt().toString()
		));
		return r;
	}

	public String getResourceType() { return resourceType; }
	public void setResourceType(String resourceType) { this.resourceType = resourceType; }
	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public Map<String, Object> getAttributes() { return attributes; }
	public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
