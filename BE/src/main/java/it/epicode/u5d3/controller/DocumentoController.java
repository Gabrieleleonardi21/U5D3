package it.epicode.u5d3.controller;

import it.epicode.u5d3.model.Documento;
import it.epicode.u5d3.payload.ScanResponse;
import it.epicode.u5d3.service.DocumentoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/document")
public class DocumentoController {

	private final DocumentoService documentoService;

	public DocumentoController(DocumentoService documentoService) {
		this.documentoService = documentoService;
	}

	/** GET /api/document/all -> tutti i documenti salvati. */
	@GetMapping("/all")
	public List<Documento> getAll() {
		return documentoService.getAll();
	}

	/**
	 * POST /api/document/scan -> anteprima: estrae il testo e lo restituisce
	 * senza salvare niente.
	 *
	 * consumes MULTIPART_FORM_DATA: un file non viaggia come JSON, il browser
	 * lo manda come form. Senza questa riga si ottiene "Unsupported Media Type".
	 */
	@PostMapping(path = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ScanResponse scan(
			@RequestParam("file") MultipartFile file,
			// required = false: se il front-end non specifica la lingua,
			// il service usa quella di default delle properties.
			@RequestParam(name = "lingua", required = false) String lingua) {
		return documentoService.scan(file, lingua);
	}

	/**
	 * POST /api/document/upload -> salva il file nel filesystem e il documento
	 * (metadati + testo estratto) sul database.
	 *
	 * 201 CREATED e non 200: la richiesta ha creato una risorsa nuova.
	 */
	@PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public Documento upload(
			@RequestParam("file") MultipartFile file,
			@RequestParam(name = "lingua", required = false) String lingua) {
		return documentoService.carica(file, lingua);
	}
}
