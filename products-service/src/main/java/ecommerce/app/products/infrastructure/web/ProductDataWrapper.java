package ecommerce.app.products.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDataWrapper {
	@NotNull
	@Valid
	private DataHolder data;

	public DataHolder getData() { return data; }
	public void setData(DataHolder data) { this.data = data; }

	public ProductRequest getAttributes() {
		return data != null ? data.getAttributes() : null;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DataHolder {
		private String type;
		@JsonProperty("attributes")
		@Valid
		private ProductRequest attributes;

		public String getType() { return type; }
		public void setType(String type) { this.type = type; }
		public ProductRequest getAttributes() { return attributes; }
		public void setAttributes(ProductRequest attributes) { this.attributes = attributes; }
	}
}
