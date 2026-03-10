package ecommerce.app.inventory.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {
	private String type;
	private UUID id;
	private Map<String, Object> attributes;

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public Map<String, Object> getAttributes() { return attributes; }
	public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
