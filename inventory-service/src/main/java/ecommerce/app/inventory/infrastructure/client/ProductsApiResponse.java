package ecommerce.app.inventory.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductsApiResponse {
	private ProductDto data;

	public ProductDto getData() { return data; }
	public void setData(ProductDto data) { this.data = data; }
}
