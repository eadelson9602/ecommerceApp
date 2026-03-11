package ecommerce.app.products.infrastructure.web;

import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.products.domain.SkuAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationExceptionHandlerTest {

	private final ValidationExceptionHandler handler = new ValidationExceptionHandler();

	@Test
	void handleSkuAlreadyExists_returns409WithJsonApiError() {
		SkuAlreadyExistsException ex = new SkuAlreadyExistsException("SKU-1");

		ResponseEntity<JsonApiDocument<?>> response = handler.handleSkuAlreadyExists(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getErrors()).hasSize(1);
		assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("SKU_ALREADY_EXISTS");
		assertThat(response.getBody().getErrors().get(0).getStatus()).isEqualTo("409");
	}

	@Test
	void handleGeneric_whenMessageNotNull_returns500WithMessage() {
		Exception ex = new RuntimeException("Something broke");

		ResponseEntity<JsonApiDocument<?>> response = handler.handleGeneric(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getErrors()).hasSize(1);
		assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("INTERNAL_ERROR");
		assertThat(response.getBody().getErrors().get(0).getDetail()).isEqualTo("Something broke");
	}

	@Test
	void handleGeneric_whenMessageNull_returns500WithDefaultDetail() {
		Exception ex = new Exception();

		ResponseEntity<JsonApiDocument<?>> response = handler.handleGeneric(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getErrors()).hasSize(1);
		assertThat(response.getBody().getErrors().get(0).getDetail()).isEqualTo("An unexpected error occurred");
	}
}
