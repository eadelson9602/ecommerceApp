package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.app.inventory.domain.Inventory;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryResource {
	public static final String TYPE = "inventory";

	private String type = TYPE;
	private UUID id;
	private Map<String, Object> attributes;

	public static InventoryResource from(Inventory inv) {
		InventoryResource r = new InventoryResource();
		r.setType(TYPE);
		r.setId(inv.getProductId());
		r.setAttributes(Map.of(
				"productId", inv.getProductId().toString(),
				"available", inv.getAvailable(),
				"reserved", inv.getReserved(),
				"version", inv.getVersion()
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
