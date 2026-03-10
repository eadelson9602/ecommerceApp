package ecommerce.app.products.infrastructure.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO de entrada HTTP (JSON:API attributes) para crear/actualizar producto.
 */
public class ProductRequest {
	@NotBlank(message = "sku is required")
	@Size(max = 64)
	private String sku;

	@NotBlank(message = "name is required")
	@Size(max = 255)
	private String name;

	@NotNull(message = "price is required")
	@DecimalMin(value = "0", inclusive = true, message = "price must be >= 0")
	@Digits(integer = 15, fraction = 4)
	private BigDecimal price;

	@NotNull(message = "status is required")
	@Pattern(regexp = "ACTIVE|INACTIVE", message = "status must be ACTIVE or INACTIVE")
	private String status;

	public String getSku() { return sku; }
	public void setSku(String sku) { this.sku = sku; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public BigDecimal getPrice() { return price; }
	public void setPrice(BigDecimal price) { this.price = price; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
}
