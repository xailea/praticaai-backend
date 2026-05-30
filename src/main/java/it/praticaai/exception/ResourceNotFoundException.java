package it.praticaai.exception;

// ── Risorsa non trovata (HTTP 404) ───────────────────────────────────────────
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " non trovato con id: " + id);
    }
}
