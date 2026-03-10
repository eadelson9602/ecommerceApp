package ecommerce.app.inventory.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(3)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Value("${app.jwt.secret:default-secret-key-min-256-bits-for-hs256-please-change-in-production}")
	private String jwtSecret;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}
		String auth = request.getHeader("Authorization");
		if (auth != null && auth.startsWith("Bearer ")) {
			String token = auth.substring(7);
			try {
				SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
				Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
				String sub = claims.getSubject();
				@SuppressWarnings("unchecked")
				List<String> roles = claims.get("roles", List.class);
				List<SimpleGrantedAuthority> authorities = roles != null
						? roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList())
						: List.of(new SimpleGrantedAuthority("ROLE_USER"));
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(sub, null, authorities));
			} catch (Exception ignored) { }
		}
		filterChain.doFilter(request, response);
	}
}
