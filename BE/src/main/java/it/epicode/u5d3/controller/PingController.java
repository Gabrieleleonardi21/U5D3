package it.epicode.u5d3.controller;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint di prova: serve solo a verificare che il collegamento FE -> BE
 * e la configurazione CORS funzionino, prima di scrivere la logica vera.
 */
@RestController
@RequestMapping("/api")
public class PingController {

	@GetMapping("/ping")
	public Map<String, Object> ping() {
		return Map.of(
				"messaggio", "pong",
				"orario", LocalDateTime.now());
	}
}
