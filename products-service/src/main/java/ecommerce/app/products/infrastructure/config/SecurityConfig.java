package ecommerce.app.products.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad: actuator y OpenAPI públicos; /api/** exigen autenticación (API Key o JWT).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
			JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll()
				)
				.addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
