package ecommerce.app.inventory.infrastructure.web;

import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.jsonapi.JsonApiError;
import ecommerce.app.inventory.infrastructure.client.ProductsServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class InventoryExceptionHandler {

	@ExceptionHandler(ProductsServiceUnavailableException.class)
	public ResponseEntity<JsonApiDocument<?>> handleProductsUnavailable(ProductsServiceUnavailableException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(JsonApiDocument.builder()
						.errors(List.of(JsonApiError.builder()
								.status("503")
								.code("PRODUCTS_SERVICE_UNAVAILABLE")
								.title("Service Unavailable")
								.detail("El servicio de productos no está disponible. Reintente más tarde.")
								.build()))
						.build());
	}
}
