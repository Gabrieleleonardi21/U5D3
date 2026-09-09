package it.epicode.u5d3.payload;

/**
 * Risposta di /scan: solo il testo letto dall'OCR, senza id ne' path,
 * perche' a quel punto non e' stato salvato ancora niente.
 */
public record ScanResponse(String titolo, String text) {
}
