package it.epicode.u5d3.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Un documento caricato: i metadati stanno qui, il file vero sta su disco
 * (o su un servizio esterno) all'indirizzo indicato da "path".
 */
@Entity
@Table(name = "documenti")
@Getter
// @Setter sulla classe genera i setter per TUTTI i campi: sotto, i due campi
// che non devono essere modificati dall'esterno lo disattivano singolarmente.
@Setter
public class Documento {

	/**
	 * Con un campo UUID, @GeneratedValue fa generare l'identificativo a Hibernate
	 * (UUID casuale) prima dell'INSERT.
	 *
	 * AccessLevel.NONE: nessun setId(). L'id lo assegna Hibernate e cambiarlo a
	 * mano su un'entita' gia' salvata gli farebbe perdere di vista la riga
	 * corrispondente, con UPDATE che non aggiornano niente o INSERT duplicati.
	 */
	@Id
	@GeneratedValue
	@Setter(AccessLevel.NONE)
	private UUID id;

	// String -> varchar(255). length lo rende esplicito nella tabella generata.
	@Column(nullable = false, length = 255)
	private String titolo;

	// Percorso/URL del file. Non e' il file: qui salviamo solo dove trovarlo.
	@Column(unique = true,nullable = false)
	private String path;

	// Dimensione del file in byte: Long e non Integer perche' oltre i 2 GB
	// un int andrebbe in overflow e restituirebbe un peso negativo.
	@Column(nullable = false)
	private Long peso;

	/**
	 * Instant e' un istante in UTC: non porta con se' il fuso orario, quindi
	 * il dato resta lo stesso anche se il server cambia timezone.
	 *
	 * updatable = false lo protegge lato database, AccessLevel.NONE lato Java:
	 * senza il secondo, un setCreatedAt() pubblico permetterebbe di falsificare
	 * la data di creazione prima del salvataggio.
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	/**
	 * columnDefinition = "TEXT" perche' il contenuto puo' superare i 255 caratteri
	 * del varchar di default. Da preferire a @Lob: su PostgreSQL @Lob su una String
	 * crea una colonna "oid" (large object gestito a parte) e in lettura
	 * si finisce facilmente con l'errore "Bad value for type long".
	 */
	@Column(columnDefinition = "TEXT")
	private String text;

	/**
	 * Costruttore vuoto richiesto da Hibernate, che crea l'oggetto con questo e
	 * poi riempie i campi quando legge una riga dal database.
	 *
	 * protected e non public: serve a Hibernate, non a noi. Cosi' nel resto del
	 * codice l'unico modo di creare un Documento e' il costruttore sotto, che
	 * obbliga a passare i dati e non lascia in giro oggetti mezzi vuoti.
	 */
	protected Documento() {
	}

	/**
	 * Il costruttore da usare nel codice: id e createdAt non compaiono perche'
	 * non li decide chi crea l'oggetto (li mettono Hibernate e @PrePersist).
	 */
	public Documento(String titolo, String path, Long peso, String text) {
		this.titolo = titolo;
		this.path = path;
		this.peso = peso;
		this.text = text;
	}

	/**
	 * @PrePersist: Hibernate chiama questo metodo subito prima dell'INSERT.
	 * Cosi' la data la decide il server e non arriva dal client, che potrebbe
	 * mandarne una sbagliata o falsificata.
	 */
	@PrePersist
	private void impostaCreatedAt() {
		this.createdAt = Instant.now();
	}
}
