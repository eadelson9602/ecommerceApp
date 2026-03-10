package ecommerce.app.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autenticación para comunicación entre microservicios: si X-API-Key es válida, marca la petición como autenticada.
 */
@Component
@Order(3)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-Key";

	@Value("${app.internal.api-key:}")
	private String internalApiKey;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		if (internalApiKey != null && !internalApiKey.isBlank()) {
			String key = request.getHeader(HEADER);
			if (internalApiKey.equals(key)) {
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken("internal", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
			}
		}
		filterChain.doFilter(request, response);
	}
}
