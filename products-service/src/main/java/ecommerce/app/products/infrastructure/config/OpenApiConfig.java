package ecommerce.app.products.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración OpenAPI 3 (Springdoc) para documentación profesional de la API.
 * Swagger UI: /swagger-ui.html — OpenAPI JSON: /v3/api-docs
 *
 * @see <a href="https://springdoc.org/">springdoc-openapi</a>
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		final String bearerAuth = "bearerAuth";
		final String apiKeyAuth = "apiKey";
		return new OpenAPI()
				.info(new Info()
						.title("Products Service API")
						.version("1.0")
						.description("API del catálogo de productos. Formato **JSON:API** (Content-Type y Accept: `application/vnd.api+json`). " +
								"Autenticación: JWT (Bearer) para frontend o API Key (header `X-API-Key`) para comunicación entre servicios.")
						.contact(new Contact().name("E-commerce Backend"))
						.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
				.components(new Components()
						.addSecuritySchemes(bearerAuth,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Token obtenido de POST /auth/login"))
						.addSecuritySchemes(apiKeyAuth,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.HEADER)
										.name("X-API-Key")
										.description("API Key para llamadas servicio a servicio")))
				.addSecurityItem(new SecurityRequirement().addList(bearerAuth).addList(apiKeyAuth));
	}
}
