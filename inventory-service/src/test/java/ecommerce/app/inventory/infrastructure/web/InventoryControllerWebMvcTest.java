package ecommerce.app.inventory.infrastructure.web;

import ecommerce.app.inventory.application.port.in.GetInventoryUseCase;
import ecommerce.app.inventory.application.port.in.ListPurchasesUseCase;
import ecommerce.app.inventory.application.port.in.ProcessPurchaseUseCase;
import ecommerce.app.inventory.application.port.in.SetInventoryUseCase;
import ecommerce.app.inventory.application.port.out.IdempotencyRecordPort;
import ecommerce.app.inventory.domain.IdempotencyRecord;
import ecommerce.app.inventory.application.model.PurchaseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests aislados del controlador para cubrir ramas que requieren mocks (CONFLICT, IDEMPOTENT_RESPONSE con JSON inválido).
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerWebMvcTest {

	private static final String JSON_API = "application/vnd.api+json";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private GetInventoryUseCase getInventoryUseCase;
	@MockBean
	private ProcessPurchaseUseCase processPurchaseUseCase;
	@MockBean
	private SetInventoryUseCase setInventoryUseCase;
	@MockBean
	private ListPurchasesUseCase listPurchasesUseCase;
	@MockBean
	private IdempotencyRecordPort idempotencyRecordPort;

	@Test
	@WithMockUser
	void purchase_whenConflict_returns409() throws Exception {
		UUID productId = UUID.randomUUID();
		when(processPurchaseUseCase.processPurchase(eq(productId), anyInt(), any()))
				.thenReturn(PurchaseResult.conflict("Optimistic lock failure"));

		String body = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":1}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors[0].code").value("CONFLICT"));
	}

	@Test
	@WithMockUser
	void purchase_whenIdempotentResponseWithInvalidCachedBody_returns500() throws Exception {
		UUID productId = UUID.randomUUID();
		IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
		idempotencyRecord.setResponseStatus(201);
		idempotencyRecord.setResponseBody("not-valid-json{{{");
		when(processPurchaseUseCase.processPurchase(eq(productId), anyInt(), any()))
				.thenReturn(PurchaseResult.fromIdempotency(idempotencyRecord));

		String body = """
				{"data":{"type":"purchases","attributes":{"productId":"%s","quantity":1}}}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchases")
						.contentType(JSON_API)
						.accept(JSON_API)
						.content(body))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errors[0].code").value("IDEMPOTENCY_ERROR"));
	}
}
