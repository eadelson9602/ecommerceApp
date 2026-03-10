package ecommerce.app.products.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ecommerce.app.products.domain.Product;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResource {
	public static final String TYPE = "products";

	@JsonProperty("type")
	private String resourceType = TYPE;
	private UUID id;
	private Map<String, Object> attributes;

	public static ProductResource from(Product p) {
		ProductResource r = new ProductResource();
		r.setResourceType(TYPE);
		r.setId(p.getId());
		r.setAttributes(Map.of(
				"sku", p.getSku(),
				"name", p.getName(),
				"price", p.getPrice(),
				"status", p.getStatus().name(),
				"createdAt", p.getCreatedAt().toString(),
				"updatedAt", p.getUpdatedAt().toString()
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
