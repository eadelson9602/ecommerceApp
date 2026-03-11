package ecommerce.app.products.infrastructure.web;

import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.jsonapi.JsonApiError;
import ecommerce.app.jsonapi.JsonApiErrorSource;
import ecommerce.app.products.domain.SkuAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ValidationExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ValidationExceptionHandler.class);

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

	@ExceptionHandler(SkuAlreadyExistsException.class)
	public ResponseEntity<JsonApiDocument<?>> handleSkuAlreadyExists(SkuAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(JsonApiDocument.builder()
						.errors(List.of(JsonApiError.builder()
								.status("409")
								.code("SKU_ALREADY_EXISTS")
								.title("Conflict")
								.detail("Ya existe un producto con el SKU indicado.")
								.build()))
						.build());
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
