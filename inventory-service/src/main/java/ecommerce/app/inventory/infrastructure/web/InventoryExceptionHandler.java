package ecommerce.app.inventory.infrastructure.web;

import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.jsonapi.JsonApiError;
import ecommerce.app.jsonapi.JsonApiErrorSource;
import ecommerce.app.inventory.infrastructure.client.ProductsServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class InventoryExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<JsonApiDocument<?>> handleValidation(MethodArgumentNotValidException ex) {
		List<JsonApiError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> JsonApiError.builder()
						.status(String.valueOf(HttpStatus.UNPROCESSABLE_ENTITY.value()))
						.code("VALIDATION_FAILED")
						.title("Unprocessable Entity")
						.detail(fe.getDefaultMessage())
						.source(new JsonApiErrorSource("/data/attributes/" + fe.getField(), null))
						.build())
				.toList();
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(JsonApiDocument.builder().errors(errors).build());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<JsonApiDocument<?>> handleGeneric(Exception ex) {
		log.error("Unhandled error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(JsonApiDocument.builder()
						.errors(List.of(JsonApiError.builder()
								.status("500")
								.code("INTERNAL_ERROR")
								.title("Internal Server Error")
								.detail(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
								.build()))
						.build());
	}
}
