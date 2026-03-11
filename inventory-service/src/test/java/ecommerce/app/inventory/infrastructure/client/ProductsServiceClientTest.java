package ecommerce.app.inventory.infrastructure.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductsServiceClientTest {

	private MockWebServer server;
	private ProductsServiceClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		String baseUrl = server.url("/").toString().replaceAll("/$", "");
		WebClient.Builder builder = WebClient.builder();
		client = new ProductsServiceClient(builder, baseUrl, "test-api-key", 2000);
	}

	@AfterEach
	void tearDown() throws IOException {
		if (server != null) {
			server.shutdown();
		}
	}

	@Test
	void getProductExists_when200WithData_returnsProductId() {
		UUID productId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
		String body = """
			{"data":{"type":"product","id":"a0000000-0000-0000-0000-000000000001","attributes":{"name":"Test"}}}
			""";
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.api+json")
				.setBody(body));

		UUID result = client.getProductExists(productId).block();

		assertThat(result).isEqualTo(productId);
		assertThat(server.getRequestCount()).isEqualTo(1);
	}

	@Test
	void getProductExists_when200WithDataNull_returnsRequestedProductId() {
		UUID productId = UUID.fromString("b0000000-0000-0000-0000-000000000002");
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.api+json")
				.setBody("{}"));

		UUID result = client.getProductExists(productId).block();

		assertThat(result).isEqualTo(productId);
	}

	@Test
	void getProductExists_when404_returnsEmpty() {
		UUID productId = UUID.randomUUID();
		server.enqueue(new MockResponse().setResponseCode(404));

		UUID result = client.getProductExists(productId).block();

		assertThat(result).isNull();
		assertThat(server.getRequestCount()).isEqualTo(1);
	}

	@Test
	void getProductExists_when500_throwsProductsServiceUnavailableException() {
		UUID productId = UUID.randomUUID();
		server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Error"));

		assertThatThrownBy(() -> client.getProductExists(productId).block())
				.isInstanceOf(ProductsServiceUnavailableException.class)
				.hasMessageContaining("Products service unavailable");
	}

	@Test
	void getProductExists_whenTimeout_throwsProductsServiceUnavailableException() {
		UUID productId = UUID.randomUUID();
		server.enqueue(new MockResponse()
				.setBody("{}")
				.setBodyDelay(5, TimeUnit.SECONDS));

		ProductsServiceClient shortTimeoutClient = new ProductsServiceClient(
				WebClient.builder(),
				server.url("/").toString().replaceAll("/$", ""),
				"key",
				100
		);

		assertThatThrownBy(() -> shortTimeoutClient.getProductExists(productId).block())
				.isInstanceOf(ProductsServiceUnavailableException.class);
	}
}
