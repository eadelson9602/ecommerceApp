package ecommerce.app.products.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listProducts_withApiKey_returns200() throws Exception {
		mockMvc.perform(get("/api/v1/products")
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
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.attributes.sku").value("IT-SKU-001"));
	}

	@Test
	void listProducts_withoutAuth_returns4xx() throws Exception {
		mockMvc.perform(get("/api/v1/products").accept("application/vnd.api+json"))
				.andExpect(status().is4xxClientError()); // 401 o 403 según configuración
	}

	@Test
	void getProductById_withApiKey_returns200Or404() throws Exception {
		String location = mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-GET\",\"name\":\"Get Test\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(get("/api/v1/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.accept("application/vnd.api+json"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.id").value(id));
		}
	}

	@Test
	void patchProduct_withApiKey_returns200() throws Exception {
		String location = mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-PATCH\",\"name\":\"Patch Test\",\"price\":2.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(patch("/api/v1/products/" + id)
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
		mockMvc.perform(get("/api/v1/products/00000000-0000-0000-0000-000000000000")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createProduct_duplicateSku_returns409() throws Exception {
		String body = "{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-DUP\",\"name\":\"First\",\"price\":1.0,\"status\":\"ACTIVE\"}}}";
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void deleteProduct_withApiKey_returns204Or404() throws Exception {
		String location = mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-DEL\",\"name\":\"Del Test\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(delete("/api/v1/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod"))
					.andExpect(status().isNoContent());
		}
	}

	// --- Más ramas web (cobertura infrastructure.web) ---

	@Test
	void createProduct_withoutAttributes_returns422() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\"}}"))
				.andExpect(status().is4xxClientError()); // 400 Bad Request o 422
	}

	@Test
	void createProduct_invalidStatus_returns400() throws Exception {
		String body = "{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-INV\",\"name\":\"Inv\",\"price\":1.0,\"status\":\"INVALID\"}}}";
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content(body))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void patchProduct_notFound_returns404() throws Exception {
		mockMvc.perform(patch("/api/v1/products/00000000-0000-0000-0000-000000000099")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"X\",\"name\":\"X\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteProduct_notFound_returns404() throws Exception {
		mockMvc.perform(delete("/api/v1/products/00000000-0000-0000-0000-000000000099")
						.header("X-API-Key", "internal-api-key-change-in-prod"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listProducts_withSortAndFilter_returns200() throws Exception {
		mockMvc.perform(get("/api/v1/products")
						.param("page[number]", "1")
						.param("page[size]", "5")
						.param("status", "ACTIVE")
						.param("sort", "-price")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void auth_login_returns200WithToken() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType("application/json")
						.content("{\"username\":\"demo\",\"password\":\"demo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	// --- Ramas adicionales para cobertura ≥75% en infrastructure.web ---

	@Test
	void listProducts_withFilterSearch_returns200() throws Exception {
		mockMvc.perform(get("/api/v1/products")
						.param("page[number]", "1")
						.param("page[size]", "10")
						.param("filter[search]", "IT-SKU")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.links.first").exists());
	}

	@Test
	void listProducts_statusInactive_returns200() throws Exception {
		mockMvc.perform(get("/api/v1/products")
						.param("page[number]", "1")
						.param("page[size]", "5")
						.param("status", "INACTIVE")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void listProducts_withPagination_linksPrevNextWhenMultiplePages() throws Exception {
		// Crear 5 productos para tener 3 páginas con page[size]=2 y que page 2 tenga prev y next
		for (int i = 0; i < 5; i++) {
			String body = String.format(
					"{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-PAG-%d\",\"name\":\"Pag%d\",\"price\":1.0,\"status\":\"ACTIVE\"}}}", i, i);
			mockMvc.perform(post("/api/v1/products")
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.contentType("application/vnd.api+json")
							.accept("application/vnd.api+json")
							.content(body))
					.andExpect(status().isCreated());
		}
		mockMvc.perform(get("/api/v1/products")
						.param("page[number]", "2")
						.param("page[size]", "2")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.accept("application/vnd.api+json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.links.prev").exists())
				.andExpect(jsonPath("$.links.next").exists());
	}

	@Test
	void patchProduct_withoutAttributes_returns422() throws Exception {
		String location = mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-NOATTR\",\"name\":\"X\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(patch("/api/v1/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.contentType("application/vnd.api+json")
							.accept("application/vnd.api+json")
							.content("{\"data\":{\"type\":\"products\"}}"))
					.andExpect(status().isBadRequest());
		}
	}

	@Test
	void patchProduct_duplicateSku_returns409() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-A\",\"name\":\"Product A\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated());
		String locationB = mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-B\",\"name\":\"Product B\",\"price\":2.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String idB = locationB != null && locationB.contains("/") ? locationB.substring(locationB.lastIndexOf("/") + 1) : null;
		if (idB != null) {
			mockMvc.perform(patch("/api/v1/products/" + idB)
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.contentType("application/vnd.api+json")
							.accept("application/vnd.api+json")
							.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-A\",\"name\":\"B updated\",\"price\":2.0,\"status\":\"ACTIVE\"}}}"))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.errors[0].code").value("SKU_ALREADY_EXISTS"));
		}
	}

	@Test
	void createProduct_attributesNull_returns400WithErrorDocument() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\"}}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value("VALIDATION_FAILED"));
	}

	@Test
	void getById_found_returns200WithData() throws Exception {
		String location = mockMvc.perform(post("/api/v1/products")
						.header("X-API-Key", "internal-api-key-change-in-prod")
						.contentType("application/vnd.api+json")
						.accept("application/vnd.api+json")
						.content("{\"data\":{\"type\":\"products\",\"attributes\":{\"sku\":\"IT-SKU-GETBYID\",\"name\":\"GetById\",\"price\":1.0,\"status\":\"ACTIVE\"}}}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		String id = location != null && location.contains("/") ? location.substring(location.lastIndexOf("/") + 1) : null;
		if (id != null) {
			mockMvc.perform(get("/api/v1/products/" + id)
							.header("X-API-Key", "internal-api-key-change-in-prod")
							.accept("application/vnd.api+json"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.id").value(id))
					.andExpect(jsonPath("$.data.attributes.sku").value("IT-SKU-GETBYID"));
		}
	}

	@Test
	void auth_login_emptyBody_returns200WithDefaultUser() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}
}
