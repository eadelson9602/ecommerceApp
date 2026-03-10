package ecommerce.app.products.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.app.products.domain.Product;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResource {
	public static final String TYPE = "products";

	private String type = TYPE;
	private UUID id;
	private Map<String, Object> attributes;

	public static ProductResource from(Product p) {
		ProductResource r = new ProductResource();
		r.setType(TYPE);
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

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public Map<String, Object> getAttributes() { return attributes; }
	public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
