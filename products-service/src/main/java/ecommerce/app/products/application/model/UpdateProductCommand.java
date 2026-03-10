package ecommerce.app.products.application.model;

import ecommerce.app.products.domain.ProductStatus;

import java.math.BigDecimal;

/**
 * Comando de aplicación para actualizar un producto (puerto de entrada).
 */
public class UpdateProductCommand {

	private final String sku;
	private final String name;
	private final BigDecimal price;
	private final ProductStatus status;

	public UpdateProductCommand(String sku, String name, BigDecimal price, ProductStatus status) {
		this.sku = sku;
		this.name = name;
		this.price = price;
		this.status = status;
	}

	public String getSku() { return sku; }
	public String getName() { return name; }
	public BigDecimal getPrice() { return price; }
	public ProductStatus getStatus() { return status; }
}
