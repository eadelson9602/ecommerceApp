package ecommerce.app.products.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

	@Test
	void getProductById_withApiKey_returns200Or404() throws Exception {
		String location = mockMvc.perform(post("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-GET\",\"name\":\"Get Test\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(get("/api/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.accept("application/vnd.api+json"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.id").value(id));
		}
	}

	@Test
	void patchProduct_withApiKey_returns200() throws Exception {
		String location = mockMvc.perform(post("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-PATCH\",\"name\":\"Patch Test\",\"price\":2.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(patch("/api/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.contentType("application/vnd.api+json")
							.accept("application/vnd.api+json")
							.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-PATCH\",\"name\":\"Updated\",\"price\":3.0,\"status\":\"INACTIVE\"}}}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.attributes.name").value("Updated"));
		}
	}

	@Test
	void getProductById_notFound_returns404() throws Exception {
		mockMvc.perform(get("/api/products/00000000-0000-0000-0000-000000000000")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createProduct_duplicateSku_returns409() throws Exception {
		String body = "{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-DUP\",\"name\":\"First\",\"price\":1.0,\"status\":\"ACTIVE\"}}}";
		mockMvc.perform(post("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void deleteProduct_withApiKey_returns204Or404() throws Exception {
		String location = mockMvc.perform(post("/api/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-DEL\",\"name\":\"Del Test\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(delete("/api/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod"))
					.andExpect(status().isNoContent());
		}
	}
}
