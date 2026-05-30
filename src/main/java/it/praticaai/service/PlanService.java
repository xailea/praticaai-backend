package it.praticaai.service;

import it.praticaai.config.AppProperties;
import it.praticaai.exception.PlanLimitExceededException;
import it.praticaai.model.Piano;
import it.praticaai.model.User;
import it.praticaai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Gestisce i limiti del piano Free e il reset mensile del contatore.
 *
 * Separare questa logica in un service dedicato ha due vantaggi:
 * 1. DocumentService rimane focalizzato sul flusso di analisi
 * 2. La logica di business del piano è testabile in isolamento
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    /**
     * Verifica se l'utente può analizzare un altro documento.
     * Se ha superato il limite mensile lancia PlanLimitExceededException (HTTP 402).
     * Se il mese è cambiato, azzera il contatore prima di controllare.
     *
     * @Transactional perché potremmo fare un save (reset del contatore)
     */
    @Transactional
    public void checkAndEnforceLimit(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utente non trovato: " + userId));

        // Gli utenti Pro e Famiglia non hanno limiti
        if (user.getPiano() != Piano.FREE) {
            return;
        }

        // Reset mensile: se oggi è dopo la data di reset, azzeriamo il contatore
        if (LocalDate.now().isAfter(user.getResetMese())) {
            log.info("Reset mensile contatore per userId={}", userId);
            user.setDocumentiMese(0);
            user.setResetMese(LocalDate.now().plusMonths(1).withDayOfMonth(1));
            userRepository.save(user);
        }

        int limite = appProperties.getFreePlanLimit();
        if (user.getDocumentiMese() >= limite) {
            throw new PlanLimitExceededException(
                    "Hai raggiunto il limite di " + limite + " documenti mensili del piano Free. " +
                    "Passa a Pro per analisi illimitate."
            );
        }

        log.debug("Limite OK per userId={}: {}/{}", userId, user.getDocumentiMese(), limite);
    }

    /**
     * Verifica se una funzionalità è disponibile per il piano dell'utente.
     * Usato dai controller per feature-gate (es. scadenze, export PDF).
     */
    public boolean isFeatureAvailable(UUID userId, Feature feature) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utente non trovato: " + userId));

        return switch (feature) {
            case SCADENZE, REMINDER_EMAIL, EXPORT_PDF, BOZZE_EMAIL, DASHBOARD ->
                    user.getPiano() == Piano.PRO || user.getPiano() == Piano.FAMIGLIA;
            case ANALISI_BASE ->
                    true; // disponibile per tutti
        };
    }

    /**
     * Feature disponibili per piano.
     * Enum invece di stringhe: compile-time safety nei controller.
     */
    public enum Feature {
        ANALISI_BASE,
        SCADENZE,
        REMINDER_EMAIL,
        EXPORT_PDF,
        BOZZE_EMAIL,
        DASHBOARD
    }
}
