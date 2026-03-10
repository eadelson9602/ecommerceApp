package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ecommerce.app.inventory.domain.Inventory;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryResource {
	public static final String TYPE = "inventory";

	@JsonProperty("type")
	private String resourceType = TYPE;
	private UUID id;
	private Map<String, Object> attributes;

	public static InventoryResource from(Inventory inv) {
		InventoryResource r = new InventoryResource();
		r.setResourceType(TYPE);
		r.setId(inv.getProductId());
		r.setAttributes(Map.of(
				"productId", inv.getProductId().toString(),
				"available", inv.getAvailable(),
				"reserved", inv.getReserved(),
				"version", inv.getVersion()
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
