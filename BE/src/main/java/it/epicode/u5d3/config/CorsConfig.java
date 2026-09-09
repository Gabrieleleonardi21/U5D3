package it.epicode.u5d3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS: il browser blocca una fetch verso un'origine diversa (5173 -> 3001)
 * se il server non dichiara esplicitamente di accettarla.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	// Origine del front-end, presa da application.properties (app.cors.allowed-origin).
	private final String allowedOrigin;

	public CorsConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
		this.allowedOrigin = allowedOrigin;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				// allowedOrigins (non "*") perche' con allowCredentials il jolly e' vietato:
				// meglio abituarsi all'origine esplicita fin da subito.
				.allowedOrigins(allowedOrigin)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				// "*" serve per header non "semplici" come Authorization e Content-Type:
				// senza, la preflight OPTIONS fallisce e in console si legge un errore CORS
				// anche quando il backend funziona perfettamente.
				.allowedHeaders("*")
				// Quanto il browser puo' ricordare la risposta alla preflight: una OPTIONS
				// ogni ora invece che una prima di ogni chiamata.
				.maxAge(3600);
	}
}
