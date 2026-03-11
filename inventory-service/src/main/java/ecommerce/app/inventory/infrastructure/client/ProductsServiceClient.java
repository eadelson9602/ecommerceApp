package ecommerce.app.inventory.infrastructure.client;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.UUID;

@Component
public class ProductsServiceClient {

	private final WebClient webClient;
	private final int timeoutMs;

	public ProductsServiceClient(
			WebClient.Builder builder,
			@Value("${app.products-service.url}") String baseUrl,
			@Value("${app.products-service.api-key}") String apiKey,
			@Value("${app.products-service.timeout-ms:3000}") int timeoutMs
	) {
		this.timeoutMs = timeoutMs;
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
				.responseTimeout(Duration.ofMillis(timeoutMs));
		this.webClient = builder
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.api+json")
				.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.api+json")
				.defaultHeader("X-API-Key", apiKey)
				.build()
				.mutate()
				.codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
				.build();
	}

	public Mono<UUID> getProductExists(UUID productId) {
		return webClient.get()
				.uri("/api/v1/products/{id}", productId)
				.retrieve()
				.bodyToMono(ProductsApiResponse.class)
				.timeout(Duration.ofMillis(timeoutMs))
				.map(r -> r.getData() != null ? r.getData().getId() : productId)
				.onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
				.onErrorResume(e -> Mono.error(new ProductsServiceUnavailableException("Products service unavailable: " + e.getMessage(), e)));
	}
}
