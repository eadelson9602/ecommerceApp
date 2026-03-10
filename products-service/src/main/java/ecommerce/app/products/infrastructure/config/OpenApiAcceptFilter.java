package ecommerce.app.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Fuerza Accept: application/json en rutas de Springdoc para evitar
 * HttpMediaTypeNotAcceptableException cuando hay negociación por defecto u otro filtro.
 */
@Component
@Order(1)
public class OpenApiAcceptFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();
		if (path != null && (path.startsWith("/v3") || path.startsWith("/swagger-ui"))) {
			request = new HttpServletRequestWrapper(request) {
				@Override
				public String getHeader(String name) {
					if ("Accept".equalsIgnoreCase(name)) {
						return "application/json";
					}
					return super.getHeader(name);
				}
				@Override
				public java.util.Enumeration<String> getHeaders(String name) {
					if ("Accept".equalsIgnoreCase(name)) {
						return Collections.enumeration(Collections.singletonList("application/json"));
					}
					return super.getHeaders(name);
				}
			};
		}
		filterChain.doFilter(request, response);
	}
}
