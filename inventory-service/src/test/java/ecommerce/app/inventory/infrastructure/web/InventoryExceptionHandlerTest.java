package ecommerce.app.inventory.infrastructure.web;

import ecommerce.app.inventory.infrastructure.client.ProductsServiceUnavailableException;
import ecommerce.app.jsonapi.JsonApiDocument;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryExceptionHandlerTest {

	private final InventoryExceptionHandler handler = new InventoryExceptionHandler();

	@Test
	void handleProductsUnavailable_returns503WithJsonApiError() {
		ProductsServiceUnavailableException ex = new ProductsServiceUnavailableException("Unavailable", new RuntimeException());

		ResponseEntity<JsonApiDocument<?>> response = handler.handleProductsUnavailable(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getErrors()).hasSize(1);
		assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("PRODUCTS_SERVICE_UNAVAILABLE");
		assertThat(response.getBody().getErrors().get(0).getStatus()).isEqualTo("503");
	}

	@Test
	void handleGeneric_whenMessageNotNull_returns500WithMessage() {
		Exception ex = new RuntimeException("Server error");

		ResponseEntity<JsonApiDocument<?>> response = handler.handleGeneric(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getErrors().get(0).getDetail()).isEqualTo("Server error");
	}

	@Test
	void handleGeneric_whenMessageNull_returns500WithDefaultDetail() {
		Exception ex = new Exception();

		ResponseEntity<JsonApiDocument<?>> response = handler.handleGeneric(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().getErrors().get(0).getDetail()).isEqualTo("An unexpected error occurred");
	}
}
