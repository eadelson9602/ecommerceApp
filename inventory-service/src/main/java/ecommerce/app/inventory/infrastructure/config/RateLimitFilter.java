package ecommerce.app.inventory.infrastructure.config;

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

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

	@Value("${app.rate-limit.max-requests-per-minute:100}")
	private int maxPerMinute;

	private final Map<String, Window> perIp = new ConcurrentHashMap<>();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String key = request.getHeader("X-Forwarded-For");
		if (key == null || key.isBlank()) key = request.getRemoteAddr();
		else key = key.split(",")[0].trim();
		long now = System.currentTimeMillis();
		Window w = perIp.compute(key, (k, v) -> {
			if (v == null || now - v.startMs > 60_000) return new Window(now, 1);
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

	private static class Window {
		final long startMs;
		int count;
		Window(long startMs, int count) { this.startMs = startMs; this.count = count; }
	}
}
