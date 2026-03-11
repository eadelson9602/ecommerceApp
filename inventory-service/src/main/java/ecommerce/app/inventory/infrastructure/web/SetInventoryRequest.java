package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SetInventoryRequest {

	@Min(value = 0, message = "available must be >= 0")
	private Integer available;

	@Valid
	private DataHolder data;

	public Integer getAvailable() {
		return available;
	}

	public void setAvailable(Integer available) {
		this.available = available;
	}

	public DataHolder getData() {
		return data;
	}

	public void setData(DataHolder data) {
		this.data = data;
	}

	public int getAvailableValue() {
		if (data != null && data.getAttributes() != null && data.getAttributes().getAvailable() != null) {
			return data.getAttributes().getAvailable();
		}
		return available != null ? available : 0;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DataHolder {
		private String type;
		@JsonProperty("attributes")
		@Valid
		private Attributes attributes;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Attributes getAttributes() {
			return attributes;
		}

		public void setAttributes(Attributes attributes) {
			this.attributes = attributes;
		}
	}

	public static class Attributes {
		@Min(value = 0, message = "available must be >= 0")
		private Integer available = 0;

		public Integer getAvailable() {
			return available;
		}

		public void setAvailable(Integer available) {
			this.available = available;
		}
	}
}
