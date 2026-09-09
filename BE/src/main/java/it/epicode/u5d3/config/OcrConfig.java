package it.epicode.u5d3.config;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Configurazione del motore OCR.
 */
@Configuration
public class OcrConfig {

	/**
	 * Scope "prototype": ogni richiesta ne riceve una nuova istanza.
	 * Tesseract di Tess4J NON e' thread-safe e mantiene stato (lingua, immagine
	 * corrente): un singolo bean condiviso, con due scansioni in parallelo,
	 * restituirebbe testo mescolato o farebbe crashare la libreria nativa.
	 */
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ITesseract tesseract(
			@Value("${app.ocr.datapath}") String datapath,
			@Value("${app.ocr.native-lib-path}") String nativeLibPath,
			@Value("${app.ocr.default-language}") String linguaDefault) {

		// Dice a JNA dove trovare libtesseract.dylib. Va impostato prima del primo
		// uso della libreria, altrimenti JNA la cerca solo nei percorsi di sistema
		// (dove Homebrew, su Apple Silicon, non installa nulla).
		System.setProperty("jna.library.path", nativeLibPath);

		Tesseract tesseract = new Tesseract();
		// Cartella dei .traineddata: senza, l'errore e'
		// "Could not initialize tesseract... Failed loading language".
		tesseract.setDatapath(datapath);
		tesseract.setLanguage(linguaDefault);
		return tesseract;
	}
}
