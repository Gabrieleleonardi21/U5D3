package it.epicode.u5d3.service;

import it.epicode.u5d3.model.Documento;
import it.epicode.u5d3.payload.ScanResponse;
import it.epicode.u5d3.repository.DocumentoRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Logica dei documenti: lettura OCR, salvataggio del file su disco e accesso
 * al database. Il controller si limita a ricevere la richiesta e passarla qui.
 */
@Service
public class DocumentoService {

	private final DocumentoRepository documentoRepository;
	private final OcrService ocrService;
	private final Path cartellaUpload;

	public DocumentoService(DocumentoRepository documentoRepository, OcrService ocrService,
			@Value("${app.upload.dir}") String cartellaUpload) {
		this.documentoRepository = documentoRepository;
		this.ocrService = ocrService;
		this.cartellaUpload = Paths.get(cartellaUpload);
	}

	/**
	 * Tutti i documenti salvati. findAll() e' gia' fornito da JpaRepository:
	 * scrivere una @Query per un semplice "SELECT * FROM documenti" sarebbe
	 * codice in piu' che fa esattamente la stessa cosa.
	 */
	public List<Documento> getAll() {
		return documentoRepository.findAll();
	}

	/**
	 * Anteprima: legge il testo e basta. Niente file su disco, niente riga sul
	 * database, cosi' l'utente puo' provare piu' lingue prima di decidere se
	 * salvare.
	 */
	public ScanResponse scan(MultipartFile file, String lingua) {
		controllaFile(file);
		byte[] contenuto = leggiContenuto(file);
		String estensione = estraiEstensione(file.getOriginalFilename());
		String testo = ocrService.estraiTesto(contenuto, estensione, lingua);
		return new ScanResponse(file.getOriginalFilename(), testo);
	}

	/**
	 * Salvataggio vero: il file finisce nel filesystem, i metadati e il testo
	 * estratto finiscono sul database.
	 */
	public Documento carica(MultipartFile file, String lingua) {
		controllaFile(file);
		byte[] contenuto = leggiContenuto(file);
		String nomeOriginale = file.getOriginalFilename();
		String estensione = estraiEstensione(nomeOriginale);

		// Prima l'OCR: se fallisce, non lasciamo su disco un file di cui
		// non esiste la riga corrispondente sul database.
		String testoEstratto = ocrService.estraiTesto(contenuto, estensione, lingua);

		// Nome sul disco = UUID: due utenti che caricano entrambi "scansione.png"
		// non si sovrascrivono a vicenda, e un nome inventato dal client
		// non puo' contenere "../" per scrivere fuori dalla cartella.
		Path destinazione = cartellaUpload.resolve(UUID.randomUUID() + estensione);
		salvaSuDisco(contenuto, destinazione);

		Documento documento = new Documento(
				nomeOriginale,
				destinazione.toString(),
				file.getSize(),
				testoEstratto);
		return documentoRepository.save(documento);
	}

	private void controllaFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nessun file ricevuto");
		}
	}

	private byte[] leggiContenuto(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File illeggibile", e);
		}
	}

	private void salvaSuDisco(byte[] contenuto, Path destinazione) {
		try {
			// createDirectories non fallisce se la cartella esiste gia'.
			Files.createDirectories(destinazione.getParent());
			Files.write(destinazione, contenuto);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Impossibile salvare il file", e);
		}
	}

	/** Estensione con il punto (".png"), stringa vuota quando non c'e'. */
	private String estraiEstensione(String nomeFile) {
		if (nomeFile == null) {
			return "";
		}
		int punto = nomeFile.lastIndexOf('.');
		if (punto < 0) {
			return "";
		}
		return nomeFile.substring(punto).toLowerCase();
	}
}
