package ecommerce.app.inventory.infrastructure.web;

import ecommerce.app.inventory.application.port.out.ProductsServicePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integración con la API (perfil test usa H2). ProductsServicePort se mockea para no llamar al servicio externo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

	private static final String JSON_API = "application/vnd.api+json";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProductsServicePort productsServicePort;

	@Test
	void getInventory_withoutAuth_returns4xx() throws Exception {
		mockMvc.perform(get("/api/inventory/" + UUID.randomUUID())
						.accept(JSON_API))
				.andExpect(status().is4xxClientError()); // 401 o 403 según configuración
	}

	@Test
	@WithMockUser
	void getInventory_whenProductNotFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.empty());

		mockMvc.perform(get("/api/inventory/" + productId)
						.accept(JSON_API))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(JSON_API))
				.andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
	}

	@Test
	@WithMockUser
	void getInventory_whenProductExistsButNoInventory_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));

		mockMvc.perform(get("/api/inventory/" + productId)
						.accept(JSON_API))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(JSON_API))
				.andExpect(jsonPath("$.errors[0].code").value("INVENTORY_NOT_FOUND"));
	}

	@Test
	@WithMockUser
	void setInventory_andGetInventory_returns200() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));

		String putBody = "{\"available\": 100}";
		mockMvc.perform(put("/api/inventory/" + productId)
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(putBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attributes.available").value(100));

		mockMvc.perform(get("/api/inventory/" + productId)
						.accept(JSON_API))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attributes.available").value(100));
	}

	@Test
	@WithMockUser
	void setInventory_bodyWithoutAvailableKey_usesZero() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));

		mockMvc.perform(put("/api/inventory/" + productId)
						.contentType(JSON_API)
						.accept(JSON_API)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attributes.available").value(0));
	}

	@Test
	@WithMockUser
	void setInventory_withNegativeAvailable_returns400() throws Exception {
		UUID productId = UUID.randomUUID();
		String putBody = "{\"available\": -1}";

		mockMvc.perform(put("/api/inventory/" + productId)
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(putBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value("VALIDATION_FAILED"));
	}

	@Test
	@WithMockUser
	void purchase_whenProductExistsAndStockSufficient_returns201() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));

		// Crear inventario
		mockMvc.perform(put("/api/inventory/" + productId)
						.contentType(JSON_API)
						.accept(JSON_API)
						.content("{\"available\": 20}"))
				.andExpect(status().isOk());

		String purchaseBody = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":5}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(purchaseBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.attributes.quantity").value(5))
				.andExpect(jsonPath("$.data.attributes.productId").value(productId.toString()));

		// Stock restante 15
		mockMvc.perform(get("/api/inventory/" + productId).accept(JSON_API))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attributes.available").value(15));
	}

	@Test
	@WithMockUser
	void purchase_withIdempotencyKey_secondRequestReturnsCachedResponse() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));

		mockMvc.perform(put("/api/inventory/" + productId)
						.contentType(JSON_API)
						.accept(JSON_API)
						.content("{\"available\": 10}"))
				.andExpect(status().isOk());

		String purchaseBody = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":2}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.header(InventoryController.IDEMPOTENCY_KEY_HEADER, "idem-key-123")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(purchaseBody))
				.andExpect(status().isCreated());

		// Segunda petición con la misma clave: misma respuesta, stock no se descuenta dos veces
		mockMvc.perform(post("/api/purchases")
						.header(InventoryController.IDEMPOTENCY_KEY_HEADER, "idem-key-123")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(purchaseBody))
				.andExpect(status().isCreated());

		// Stock sigue en 8 (solo se descontó una vez)
		mockMvc.perform(get("/api/inventory/" + productId).accept(JSON_API))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attributes.available").value(8));
	}

	@Test
	@WithMockUser
	void purchase_withoutProductId_returns400() throws Exception {
		String body = "{\"data\":{\"type\":\"purchases\",\"attributes\":{\"quantity\":1}}}";

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	void purchase_whenProductNotFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.empty());

		String purchaseBody = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":1}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(purchaseBody))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"));
	}

	@Test
	@WithMockUser
	void purchase_whenInventoryNotFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));
		// No creamos inventario con setInventory

		String purchaseBody = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":1}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(purchaseBody))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errors[0].code").value("INVENTORY_NOT_FOUND"));
	}

	@Test
	@WithMockUser
	void purchase_whenInsufficientStock_returns422() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productsServicePort.getProductExists(any(UUID.class))).thenReturn(Mono.just(productId));

		mockMvc.perform(put("/api/inventory/" + productId)
						.contentType(JSON_API)
						.accept(JSON_API)
						.content("{\"available\": 2}"))
				.andExpect(status().isOk());

		String purchaseBody = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":5}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(purchaseBody))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errors[0].code").value("INSUFFICIENT_STOCK"));
	}

	@Test
	@WithMockUser
	void listPurchases_returns200WithDataAndMeta() throws Exception {
		mockMvc.perform(get("/api/purchases")
						.param("page[number]", "1")
						.param("page[size]", "10")
						.accept(JSON_API))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(JSON_API))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.meta.totalRecords").exists());
	}

	@Test
	void listPurchases_withoutAuth_returns4xx() throws Exception {
		mockMvc.perform(get("/api/purchases").accept(JSON_API))
				.andExpect(status().is4xxClientError());
	}
}
