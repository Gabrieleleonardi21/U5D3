package it.epicode.u5d3.payload;

import java.time.Instant;

/**
 * Forma unica delle risposte di errore: il front-end sa sempre cosa aspettarsi
 * e puo' mostrare "messaggio" all'utente senza altri controlli.
 *
 * record: classe immutabile con costruttore, getter ed equals generati dal
 * compilatore. Per un oggetto che trasporta dati e basta, Lombok non serve.
 */
public record ErrorPayload(int status, String messaggio, Instant timestamp) {

	public ErrorPayload(int status, String messaggio) {
		this(status, messaggio, Instant.now());
	}
}
