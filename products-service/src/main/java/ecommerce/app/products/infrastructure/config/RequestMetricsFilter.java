package ecommerce.app.products.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = "app.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class RequestMetricsFilter extends OncePerRequestFilter {

	private final MeterRegistry registry;

	public RequestMetricsFilter(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Timer.Sample sample = Timer.start(registry);
		try {
			filterChain.doFilter(request, response);
		} finally {
			sample.stop(Timer.builder("http.api.requests")
					.tag("method", request.getMethod())
					.tag("uri", request.getRequestURI())
					.tag("status", String.valueOf(response.getStatus()))
					.register(registry));
		}
	}
}
