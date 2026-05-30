package it.praticaai.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestione centralizzata degli errori.
 * Ogni eccezione viene mappata a un JSON uniforme:
 * {
 *   "timestamp": "...",
 *   "status": 4xx,
 *   "error": "CODICE_ERRORE",
 *   "message": "Messaggio leggibile"
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Eccezioni di business ─────────────────────────────────────────────

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handlePlanLimit(PlanLimitExceededException ex) {
        return error(HttpStatus.PAYMENT_REQUIRED, "PLAN_LIMIT_EXCEEDED", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(FileTypeNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleFileType(FileTypeNotAllowedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_TYPE_NOT_ALLOWED", ex.getMessage());
    }

    // ── Validazione input ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Il file supera la dimensione massima consentita (10 MB)");
    }

    // ── Fallback generico ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Errore non gestito", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Si è verificato un errore interno. Riprova più tardi.");
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", code,
                "message", message
        ));
    }
}
