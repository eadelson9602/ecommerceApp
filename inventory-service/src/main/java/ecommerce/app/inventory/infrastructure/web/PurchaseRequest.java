package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseRequest {
	@NotNull
	@Valid
	private DataHolder data;

	public DataHolder getData() { return data; }
	public void setData(DataHolder data) { this.data = data; }

	public UUID getProductId() {
		return data != null && data.getAttributes() != null ? data.getAttributes().getProductId() : null;
	}
	public Integer getQuantity() {
		return data != null && data.getAttributes() != null ? data.getAttributes().getQuantity() : null;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DataHolder {
		private String type;
		@JsonProperty("attributes")
		@Valid
		private Attributes attributes;
		public String getType() { return type; }
		public void setType(String type) { this.type = type; }
		public Attributes getAttributes() { return attributes; }
		public void setAttributes(Attributes attributes) { this.attributes = attributes; }
	}

	public static class Attributes {
		@NotNull(message = "productId is required")
		private UUID productId;
		@NotNull(message = "quantity is required")
		@Positive(message = "quantity must be positive")
		private Integer quantity;
		public UUID getProductId() { return productId; }
		public void setProductId(UUID productId) { this.productId = productId; }
		public Integer getQuantity() { return quantity; }
		public void setQuantity(Integer quantity) { this.quantity = quantity; }
	}
}
