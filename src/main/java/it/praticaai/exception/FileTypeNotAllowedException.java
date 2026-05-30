package it.praticaai.exception;

// ── Tipo file non consentito (HTTP 415) ──────────────────────────────────────
public class FileTypeNotAllowedException extends RuntimeException {
    public FileTypeNotAllowedException(String mimeType) {
        super("Tipo file non supportato: " + mimeType + ". Accettati: PDF, JPG, PNG");
    }
}
