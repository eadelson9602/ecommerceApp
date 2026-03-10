package ecommerce.app.inventory.infrastructure.client;

/**
 * Excepción de infraestructura: el servicio de productos no está disponible.
 */
public class ProductsServiceUnavailableException extends RuntimeException {

	public ProductsServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
