package ecommerce.app.products.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integración con la API (perfil test usa H2). Con Docker disponible, ejecutar
 * también ProductControllerTestcontainersIT (tag testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listProducts_withApiKey_returns200() throws Exception {
		mockMvc.perform(get("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/vnd.api+json"))
				.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void createProduct_withApiKey_returns201() throws Exception {
		String body = """
				{"data":{"type":"products","attributes":{"sku":"IT-SKU-001","name":"Test Product","price":9.99,"status":"ACTIVE"}}}
				""";
		mockMvc.perform(post("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.attributes.sku").value("IT-SKU-001"));
	}

	@Test
	void listProducts_withoutAuth_returns4xx() throws Exception {
		mockMvc.perform(get("/api/products").accept("application/vnd.api+json"))
				.andExpect(status().is4xxClientError()); // 401 o 403 según configuración
	}
}
