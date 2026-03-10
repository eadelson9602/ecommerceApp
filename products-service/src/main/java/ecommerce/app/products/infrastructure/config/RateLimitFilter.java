package ecommerce.app.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit básico por IP (ventana fija 1 minuto).
 * Respuesta 429 si se supera el límite.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

	@Value("${app.rate-limit.max-requests-per-minute:100}")
	private int maxPerMinute;

	private final Map<String, Window> perIp = new ConcurrentHashMap<>();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String key = clientKey(request);
		long now = System.currentTimeMillis();
		Window w = perIp.compute(key, (k, v) -> {
			if (v == null || now - v.startMs > 60_000) {
				return new Window(now, 1);
			}
			v.count++;
			return v;
		});
		if (w.count > maxPerMinute) {
			response.setStatus(429);
			response.setContentType("application/vnd.api+json");
			response.getWriter().write("{\"errors\":[{\"status\":\"429\",\"code\":\"RATE_LIMIT\",\"title\":\"Too Many Requests\",\"detail\":\"Rate limit exceeded.\"}]}");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private String clientKey(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private static class Window {
		final long startMs;
		int count;
		Window(long startMs, int count) {
			this.startMs = startMs;
			this.count = count;
		}
	}
}
