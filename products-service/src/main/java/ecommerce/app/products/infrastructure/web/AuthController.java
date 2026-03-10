package ecommerce.app.products.infrastructure.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Login para el frontend: devuelve un JWT válido. No requiere autenticación previa.
 * Documentación interactiva: Swagger UI (/swagger-ui.html).
 */
@Tag(name = "Auth", description = "Autenticación y obtención de JWT")
@RestController
@RequestMapping("/auth")
public class AuthController {

	@Value("${app.jwt.secret:default-secret-key-min-256-bits-for-hs256-please-change-in-production}")
	private String jwtSecret;

	@Value("${app.jwt.expiration-seconds:3600}")
	private long expirationSeconds;

	@Operation(summary = "Login", description = "Devuelve un JWT (accessToken) para usar en header Authorization: Bearer <token>. Body: username, password (demo acepta cualquiera).")
	@ApiResponse(responseCode = "200", description = "Token JWT generado",
			content = @Content(mediaType = "application/json",
					examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbG...\",\"tokenType\":\"Bearer\"}")))
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(
			@RequestBody Map<String, String> body) {
		// Acepta cualquier usuario/contraseña para demo; en producción validar contra BD/LDAP
		String username = body != null ? body.getOrDefault("username", "user") : "user";
		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now();
		String token = Jwts.builder()
				.subject(username)
				.claim("roles", List.of("USER"))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(expirationSeconds)))
				.signWith(key)
				.compact();
		return ResponseEntity.ok(Map.of("accessToken", token, "tokenType", "Bearer"));
	}
}
