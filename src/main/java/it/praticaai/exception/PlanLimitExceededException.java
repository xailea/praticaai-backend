package it.praticaai.exception;

// ── Limite piano superato (HTTP 402) ────────────────────────────────────────
public class PlanLimitExceededException extends RuntimeException {
    public PlanLimitExceededException(String message) {
        super(message);
    }
}
