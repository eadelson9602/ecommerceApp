package ecommerce.app.products.infrastructure.web;

import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.jsonapi.JsonApiError;
import ecommerce.app.jsonapi.JsonApiErrorSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ValidationExceptionHandler {

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
}
