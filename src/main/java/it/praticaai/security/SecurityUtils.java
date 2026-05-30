package it.praticaai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility statica per estrarre l'ID dell'utente autenticato dal SecurityContext.
 *
 * Uso nei service layer:
 *   UUID userId = SecurityUtils.getAuthUserId();
 *
 * Evita di passare l'userId come parametro attraverso tutti i layer:
 * lo leggiamo direttamente dal contesto di sicurezza.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Ritorna l'UUID dell'utente autenticato.
     * @throws IllegalStateException se non c'è autenticazione (non dovrebbe
     *         mai accadere su endpoint protetti, ma meglio essere espliciti)
     */
    public static UUID getAuthUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Nessun utente autenticato nel contesto di sicurezza");
        }
        return (UUID) auth.getPrincipal();
    }
}
