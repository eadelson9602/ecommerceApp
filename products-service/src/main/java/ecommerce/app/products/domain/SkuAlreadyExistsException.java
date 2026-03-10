package ecommerce.app.products.domain;

public class SkuAlreadyExistsException extends RuntimeException {

	public SkuAlreadyExistsException(String sku) {
		super("SKU already exists: " + sku);
	}
}
