package ecommerce.app.products.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración Web MVC. No se establece defaultContentType para no afectar
 * /v3/api-docs y /swagger-ui. El filtro OpenApiAcceptFilter fuerza Accept: application/json
 * en esas rutas. La API (/api/products) declara produces en cada endpoint.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
