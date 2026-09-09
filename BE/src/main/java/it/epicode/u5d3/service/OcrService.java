package it.epicode.u5d3.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Estrae il testo da un documento. Lavora sui byte: non sa da dove arrivano
 * (upload, disco, un altro servizio) e non tocca il database.
 */
@Service
public class OcrService {

	// ObjectProvider e non ITesseract diretto: il bean e' prototype, quindi va
	// chiesto al momento dell'uso. Iniettandolo normalmente in questo service,
	// che e' singleton, ne riceveremmo UNA sola istanza riusata per sempre.
	private final ObjectProvider<ITesseract> tesseractProvider;
	private final String linguaDefault;

	public OcrService(ObjectProvider<ITesseract> tesseractProvider,
			@Value("${app.ocr.default-language}") String linguaDefault) {
		this.tesseractProvider = tesseractProvider;
		this.linguaDefault = linguaDefault;
	}

	/**
	 * @param contenuto i byte del file (immagine o PDF)
	 * @param estensione serve a dare un nome sensato al file temporaneo usato
	 *                   per i PDF: Tess4J sceglie come leggerlo dall'estensione
	 * @param lingua codice Tesseract a tre lettere: ita, eng, deu, fra...
	 */
	public String estraiTesto(byte[] contenuto, String estensione, String lingua) {
		String linguaScelta = linguaDefault;
		if (lingua != null && !lingua.isBlank()) {
			linguaScelta = lingua;
		}

		ITesseract tesseract = tesseractProvider.getObject();
		tesseract.setLanguage(linguaScelta);

		BufferedImage immagine = leggiComeImmagine(contenuto);
		// ImageIO restituisce null per cio' che non e' un'immagine (tipicamente
		// un PDF): in quel caso passiamo il file a Tess4J, che lo rende in pagine
		// con PDFBox.
		if (immagine == null) {
			return ocrDaFile(tesseract, contenuto, estensione);
		}
		return ocrDaImmagine(tesseract, immagine);
	}

	private String ocrDaImmagine(ITesseract tesseract, BufferedImage immagine) {
		try {
			return tesseract.doOCR(appiattisciTrasparenza(immagine));
		} catch (TesseractException e) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"OCR fallito: " + e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw linguaNonDisponibile(e);
		}
	}

	private String ocrDaFile(ITesseract tesseract, byte[] contenuto, String estensione) {
		// Tess4J legge da File, non da array di byte: scriviamo un temporaneo e
		// lo cancelliamo subito dopo, qualunque cosa succeda (finally).
		Path temporaneo = null;
		try {
			temporaneo = Files.createTempFile("ocr-", estensione);
			Files.write(temporaneo, contenuto);
			return tesseract.doOCR(temporaneo.toFile());
		} catch (TesseractException e) {
			// File illeggibile o libreria nativa mancante.
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"OCR fallito: " + e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw linguaNonDisponibile(e);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Impossibile scrivere il file temporaneo", e);
		} finally {
			eliminaTemporaneo(temporaneo);
		}
	}

	/**
	 * Tess4J segnala una lingua non installata con IllegalArgumentException
	 * ("Specified language data does not exist"). Senza intercettarla, una
	 * richiesta con lingua=xyz risponderebbe 500, come se il server fosse rotto:
	 * e' invece un dato sbagliato mandato dal client, quindi 400.
	 */
	private ResponseStatusException linguaNonDisponibile(IllegalArgumentException e) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Lingua non disponibile: controlla il codice a tre lettere (ita, eng, deu...)", e);
	}

	private BufferedImage leggiComeImmagine(byte[] contenuto) {
		try {
			return ImageIO.read(new ByteArrayInputStream(contenuto));
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Ridisegna l'immagine su fondo bianco togliendo il canale alpha.
	 *
	 * Serve davvero: un PNG con sfondo trasparente (uno screenshot, un ritaglio)
	 * arriva a Tesseract con l'alpha interpretato come nero, quindi testo nero
	 * su fondo nero. Il risultato non e' un errore, e' peggio: qualche carattere
	 * a caso al posto del testo, e sembra che l'OCR "funzioni male".
	 */
	private BufferedImage appiattisciTrasparenza(BufferedImage originale) {
		if (originale.getTransparency() == Transparency.OPAQUE) {
			return originale;
		}

		BufferedImage senzaAlpha = new BufferedImage(
				originale.getWidth(), originale.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D grafica = senzaAlpha.createGraphics();
		grafica.setColor(Color.WHITE);
		grafica.fillRect(0, 0, originale.getWidth(), originale.getHeight());
		grafica.drawImage(originale, 0, 0, null);
		// dispose libera le risorse native della grafica: senza, sotto carico
		// la memoria fuori dall'heap cresce fino a far rallentare l'applicazione.
		grafica.dispose();
		return senzaAlpha;
	}

	private void eliminaTemporaneo(Path temporaneo) {
		if (temporaneo == null) {
			return;
		}
		try {
			Files.deleteIfExists(temporaneo);
		} catch (IOException e) {
			// Un temporaneo rimasto non giustifica il fallimento della richiesta:
			// l'utente ha gia' il suo testo estratto.
			temporaneo.toFile().deleteOnExit();
		}
	}
}
