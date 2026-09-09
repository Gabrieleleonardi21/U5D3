package it.epicode.u5d3.exceptions;

import it.epicode.u5d3.payload.ErrorPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Raccoglie le eccezioni di TUTTI i controller e le traduce in JSON.
 *
 * Senza, Spring risponde con la pagina HTML "Whitelabel Error": il front-end fa
 * risposta.json() su dell'HTML e ottiene "Unexpected token '<'", che nasconde
 * l'errore vero. In piu' la risposta di default contiene lo stack trace, cioe'
 * la struttura interna del server, che non deve uscire dall'applicazione.
 */
@RestControllerAdvice
public class ExceptionsHandler {

	private static final Logger log = LoggerFactory.getLogger(ExceptionsHandler.class);

	/**
	 * Le eccezioni che lanciamo noi dai service: portano gia' lo status giusto
	 * (400 file mancante, 422 OCR fallito...) e un messaggio scritto per l'utente.
	 */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorPayload> gestisciResponseStatus(ResponseStatusException e) {
		String messaggio = e.getReason();
		if (messaggio == null) {
			messaggio = "Richiesta non valida";
		}
		return ResponseEntity.status(e.getStatusCode())
				.body(new ErrorPayload(e.getStatusCode().value(), messaggio));
	}

	/** Parametro obbligatorio assente (es. POST /scan senza il campo "file"). */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorPayload> gestisciParametroMancante(
			MissingServletRequestParameterException e) {
		String messaggio = "Parametro mancante: " + e.getParameterName();
		return ResponseEntity.badRequest()
				.body(new ErrorPayload(HttpStatus.BAD_REQUEST.value(), messaggio));
	}

	/**
	 * File oltre il limite di spring.servlet.multipart.max-file-size.
	 * 413 e non 400: la richiesta e' valida, e' solo troppo grande.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorPayload> gestisciFileTroppoGrande(MaxUploadSizeExceededException e) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(new ErrorPayload(HttpStatus.PAYLOAD_TOO_LARGE.value(),
						"File troppo grande: il limite e' 10MB"));
	}

	/**
	 * Rete di sicurezza per tutto il resto. Il dettaglio finisce nei log del
	 * server, dove serve a noi; al client va un messaggio generico, perche'
	 * il testo di un'eccezione inattesa puo' contenere percorsi, query o
	 * altri dettagli interni.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorPayload> gestisciGenerica(Exception e) {
		// Molte eccezioni di Spring sanno gia' quale status meritano (file
		// mancante nel form, metodo HTTP sbagliato, media type non supportato):
		// lo dichiarano implementando ErrorResponse. Senza questo controllo, il
		// catch-all qui sotto le appiattirebbe tutte su 500, e una richiesta
		// sbagliata del client sembrerebbe un server rotto.
		if (e instanceof ErrorResponse errore) {
			return ResponseEntity.status(errore.getStatusCode())
					.body(new ErrorPayload(errore.getStatusCode().value(), e.getMessage()));
		}

		log.error("Errore non gestito", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorPayload(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"Errore interno del server"));
	}
}
