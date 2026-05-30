package it.praticaai.service;

import it.praticaai.config.ResendConfig;
import it.praticaai.model.Deadline;
import it.praticaai.model.Piano;
import it.praticaai.model.User;
import it.praticaai.repository.DeadlineRepository;
import it.praticaai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Scheduler giornaliero per i reminder email.
 *
 * Logica:
 *  1. Trova tutte le scadenze non notificate entro N giorni (default 7)
 *  2. Raggruppa per utente — una email per utente, non una per scadenza
 *  3. Filtra solo utenti Pro/Famiglia (i free non hanno reminder)
 *  4. Invia l'email con il template HTML
 *  5. Marca le scadenze come notificata=true
 *
 * Cron: ogni giorno alle 08:00 (ora italiana).
 * Su Render free tier il servizio va in sleep dopo 15 min di inattività:
 * lo scheduler funziona solo se il servizio è sveglio.
 * In Fase 2 si può usare un cron job esterno (es. cron-job.org)
 * per fare un ping a /api/health ogni 14 minuti.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineScheduler {

    private final DeadlineRepository  deadlineRepository;
    private final UserRepository      userRepository;
    private final EmailService        emailService;
    private final EmailTemplateService emailTemplateService;
    private final ResendConfig        resendConfig;

    /**
     * Cron expression: "0 0 8 * * *" = ogni giorno alle 08:00:00
     * Per test in dev puoi usare: "0 * * * * *" (ogni minuto)
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Rome")
    @Transactional
    public void sendDeadlineReminders() {
        log.info("=== DeadlineScheduler avviato ===");

        LocalDate entroData = LocalDate.now().plusDays(resendConfig.getReminderDaysBefore());
        List<Deadline> scadenzeInArrivo = deadlineRepository.findScadenzeToNotify(entroData);

        if (scadenzeInArrivo.isEmpty()) {
            log.info("Nessuna scadenza da notificare oggi.");
            return;
        }

        log.info("Trovate {} scadenze da notificare", scadenzeInArrivo.size());

        // Raggruppa per userId — una email aggregata per utente
        Map<UUID, List<Deadline>> perUtente = scadenzeInArrivo.stream()
                .collect(Collectors.groupingBy(d -> d.getUser().getId()));

        int emailInviate  = 0;
        int emailSkippate = 0;

        for (Map.Entry<UUID, List<Deadline>> entry : perUtente.entrySet()) {
            UUID userId = entry.getKey();
            List<Deadline> scadenze = entry.getValue();

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Utente non trovato per userId={}, skip", userId);
                continue;
            }

            // I reminder sono solo per utenti Pro e Famiglia
            if (user.getPiano() == Piano.FREE) {
                emailSkippate++;
                continue;
            }

            try {
                String subject = buildSubject(scadenze);
                String html    = emailTemplateService.buildReminderEmail(
                        user.getEmail(), scadenze
                );

                emailService.sendEmail(user.getEmail(), subject, html);

                // Marca tutte le scadenze di questo utente come notificate
                scadenze.forEach(s -> s.setNotificata(true));
                deadlineRepository.saveAll(scadenze);

                emailInviate++;

            } catch (Exception e) {
                log.error("Errore invio reminder a userId={}: {}", userId, e.getMessage(), e);
                // Continua con il prossimo utente — non bloccare l'intero scheduler
            }
        }

        log.info("=== DeadlineScheduler completato: {} email inviate, {} skip (piano free) ===",
                emailInviate, emailSkippate);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildSubject(List<Deadline> scadenze) {
        if (scadenze.size() == 1) {
            return "⏰ Scadenza in arrivo: " + scadenze.get(0).getDescrizione();
        }
        return "⏰ Hai " + scadenze.size() + " scadenze in arrivo — PraticaAI";
    }
}
