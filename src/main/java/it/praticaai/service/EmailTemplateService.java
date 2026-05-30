package it.praticaai.service;

import it.praticaai.model.Deadline;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Genera i template HTML per le email di notifica.
 *
 * Teniamo l'HTML inline per semplicità in MVP.
 * In Fase 2 si può migrare a Thymeleaf o template file separati.
 *
 * Design: email sobria, mobile-friendly, colori PraticaAI.
 */
@Service
public class EmailTemplateService {

    private static final DateTimeFormatter IT_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN);

    /**
     * Email reminder per una o più scadenze in arrivo.
     * Raggruppiamo le scadenze dello stesso utente in una sola email
     * per evitare spam (vedi DeadlineScheduler).
     */
    public String buildReminderEmail(String userName, List<Deadline> scadenze) {
        StringBuilder rows = new StringBuilder();

        for (Deadline s : scadenze) {
            long giorniRimanenti = s.getDataScadenza() != null
                    ? ChronoUnit.DAYS.between(LocalDate.now(), s.getDataScadenza())
                    : -1;

            String badgeColor = switch (s.getUrgenza()) {
                case ALTA  -> "#dc2626";   // rosso
                case MEDIA -> "#d97706";   // arancione
                case BASSA -> "#16a34a";   // verde
            };

            String dataFormattata = s.getDataScadenza() != null
                    ? s.getDataScadenza().format(IT_DATE)
                    : "Data non specificata";

            String giorniLabel = giorniRimanenti >= 0
                    ? (giorniRimanenti == 0 ? "Oggi!" : "tra " + giorniRimanenti + " giorni")
                    : "";

            rows.append("""
                    <tr>
                      <td style="padding:12px 0;border-bottom:1px solid #f0f0f0;">
                        <div style="display:flex;align-items:center;gap:8px;">
                          <span style="background:%s;color:white;font-size:11px;
                                       font-weight:600;padding:2px 8px;border-radius:12px;">
                            %s
                          </span>
                          <strong style="color:#111827;">%s</strong>
                        </div>
                        <div style="margin-top:4px;color:#6b7280;font-size:14px;">
                          📅 %s %s
                        </div>
                      </td>
                    </tr>
                    """.formatted(
                    badgeColor,
                    s.getUrgenza().name(),
                    escapeHtml(s.getDescrizione()),
                    dataFormattata,
                    giorniLabel.isEmpty() ? "" : "· <strong>" + giorniLabel + "</strong>"
            ));
        }

        return """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                </head>
                <body style="margin:0;padding:0;background:#f9fafb;font-family:-apple-system,
                             BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f9fafb;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:white;border-radius:12px;
                                    box-shadow:0 1px 3px rgba(0,0,0,.1);overflow:hidden;">

                        <!-- Header -->
                        <tr>
                          <td style="background:#2563eb;padding:28px 32px;">
                            <h1 style="margin:0;color:white;font-size:22px;font-weight:700;">
                              📋 PraticaAI
                            </h1>
                            <p style="margin:4px 0 0;color:#bfdbfe;font-size:14px;">
                              Le tue scadenze in arrivo
                            </p>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:28px 32px;">
                            <p style="margin:0 0 8px;color:#374151;">
                              Ciao <strong>%s</strong>,
                            </p>
                            <p style="margin:0 0 24px;color:#6b7280;font-size:15px;">
                              Hai %d scadenza%s imminente%s. Controlla e segna come completate
                              quelle già gestite.
                            </p>

                            <table width="100%%" cellpadding="0" cellspacing="0">
                              %s
                            </table>

                            <div style="margin-top:28px;text-align:center;">
                              <a href="https://praticaai.it/dashboard"
                                 style="background:#2563eb;color:white;text-decoration:none;
                                        padding:12px 28px;border-radius:8px;font-weight:600;
                                        display:inline-block;">
                                Vai alla Dashboard →
                              </a>
                            </div>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#f9fafb;padding:20px 32px;
                                     border-top:1px solid #e5e7eb;">
                            <p style="margin:0;color:#9ca3af;font-size:12px;text-align:center;">
                              Hai ricevuto questa email perché hai un account PraticaAI Pro.<br>
                              Per disattivare i reminder, vai in
                              <a href="https://praticaai.it/settings" style="color:#2563eb;">
                                Impostazioni → Notifiche
                              </a>.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(userName),
                scadenze.size(),
                scadenze.size() == 1 ? "" : "e",
                scadenze.size() == 1 ? "" : "i",
                rows
        );
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
