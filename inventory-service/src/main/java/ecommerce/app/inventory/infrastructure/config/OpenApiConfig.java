package ecommerce.app.inventory.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración OpenAPI 3 (Springdoc) para documentación de la API de inventario y compras.
 * Swagger UI: /swagger-ui.html — OpenAPI JSON: /v3/api-docs
 *
 * @see <a href="https://springdoc.org/">springdoc-openapi</a>
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		final String bearerAuth = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("Inventory Service API")
						.version("1.0")
						.description("API de inventario por producto y compras. Formato **JSON:API**. " +
								"Las compras (POST /purchases) son idempotentes mediante el header `Idempotency-Key`. " +
								"Autenticación: JWT (Bearer) obtenido del Products Service (/auth/login)."))
				.components(new Components()
						.addSecuritySchemes(bearerAuth,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Token de POST /auth/login (Products Service)")))
				.addSecurityItem(new SecurityRequirement().addList(bearerAuth));
	}
}
