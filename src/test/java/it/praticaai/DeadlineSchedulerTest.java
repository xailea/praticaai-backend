package it.praticaai.service;

import it.praticaai.config.ResendConfig;
import it.praticaai.model.*;
import it.praticaai.repository.DeadlineRepository;
import it.praticaai.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeadlineScheduler — logica invio reminder")
class DeadlineSchedulerTest {

    @Mock
    DeadlineRepository deadlineRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    EmailService emailService;

    @Mock
    EmailTemplateService emailTemplateService;

    @Mock
    ResendConfig resendConfig;

    @InjectMocks
    DeadlineScheduler scheduler;

    @Test
    @DisplayName("Nessuna scadenza → nessuna email inviata")
    void nessunaScadenza_nessunaEmail() {
        when(deadlineRepository.findScadenzeToNotify(any()))
                .thenReturn(List.of());

        scheduler.sendDeadlineReminders();

        verify(emailService, never()).sendEmail(any(), any(), any());
        verify(deadlineRepository, never()).saveAll(anyList());
        verifyNoInteractions(emailTemplateService);
    }

    @Test
    @DisplayName("Utente Pro con scadenza → email inviata e scadenza marcata notificata")
    void utentePro_scadenzaPresente_emailInviata() {
        User utente = utentePro("mario@test.it");
        Deadline scadenza = scadenzaPerUtente(utente);

        when(deadlineRepository.findScadenzeToNotify(any()))
                .thenReturn(List.of(scadenza));

        when(userRepository.findById(utente.getId()))
                .thenReturn(Optional.of(utente));

        when(emailTemplateService.buildReminderEmail(anyString(), anyList()))
                .thenReturn("<html>mock</html>");

        scheduler.sendDeadlineReminders();

        verify(emailTemplateService).buildReminderEmail(anyString(), anyList());
        verify(emailService).sendEmail(eq("mario@test.it"), anyString(), eq("<html>mock</html>"));
        verify(deadlineRepository).saveAll(anyList());

        assert scadenza.isNotificata();
    }

    @Test
    @DisplayName("Utente Free con scadenza → email NON inviata")
    void utenteFree_scadenzaPresente_emailNonInviata() {
        User utente = utenteFree("free@test.it");
        Deadline scadenza = scadenzaPerUtente(utente);

        when(deadlineRepository.findScadenzeToNotify(any()))
                .thenReturn(List.of(scadenza));

        when(userRepository.findById(utente.getId()))
                .thenReturn(Optional.of(utente));

        scheduler.sendDeadlineReminders();

        verify(emailService, never()).sendEmail(any(), any(), any());
        verify(deadlineRepository, never()).saveAll(anyList());
        verifyNoInteractions(emailTemplateService);
    }

    @Test
@DisplayName("Due utenti Pro: email separate per ciascuno")
void dueUtentiPro_emailSeparate() {
    User utente1 = utentePro("a@test.it");
    User utente2 = utentePro("b@test.it");

    Deadline s1 = scadenzaPerUtente(utente1);
    Deadline s2 = scadenzaPerUtente(utente2);

    when(deadlineRepository.findScadenzeToNotify(any()))
            .thenReturn(List.of(s1, s2));

    when(userRepository.findById(utente1.getId()))
            .thenReturn(Optional.of(utente1));

    when(userRepository.findById(utente2.getId()))
            .thenReturn(Optional.of(utente2));

    when(emailTemplateService.buildReminderEmail(anyString(), anyList()))
            .thenReturn("<html>mock</html>");

    scheduler.sendDeadlineReminders();

    verify(emailTemplateService, times(2))
            .buildReminderEmail(anyString(), anyList());

    verify(emailService, times(2))
            .sendEmail(anyString(), anyString(), eq("<html>mock</html>"));

    verify(deadlineRepository, times(2))
            .saveAll(anyList());

    assert s1.isNotificata();
    assert s2.isNotificata();
}

    @Test
    @DisplayName("Errore email per un utente → scheduler continua per gli altri")
    void erroreEmail_schedulerContinua() {
        User utente1 = utentePro("a@test.it");
        User utente2 = utentePro("b@test.it");

        Deadline s1 = scadenzaPerUtente(utente1);
        Deadline s2 = scadenzaPerUtente(utente2);

        when(deadlineRepository.findScadenzeToNotify(any()))
                .thenReturn(List.of(s1, s2));

        when(userRepository.findById(utente1.getId()))
                .thenReturn(Optional.of(utente1));

        when(userRepository.findById(utente2.getId()))
                .thenReturn(Optional.of(utente2));

        when(emailTemplateService.buildReminderEmail(anyString(), anyList()))
                .thenReturn("<html>mock</html>");

        doThrow(new RuntimeException("SMTP error"))
                .doNothing()
                .when(emailService)
                .sendEmail(anyString(), anyString(), anyString());

        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> scheduler.sendDeadlineReminders());

        verify(emailTemplateService, times(2)).buildReminderEmail(anyString(), anyList());
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
    }

    // ── Factory helpers ────────────────────────────────────────────────────

    private User utentePro(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .piano(Piano.PRO)
                .documentiMese(0)
                .resetMese(LocalDate.now().plusMonths(1))
                .build();
    }

    private User utenteFree(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .piano(Piano.FREE)
                .documentiMese(0)
                .resetMese(LocalDate.now().plusMonths(1))
                .build();
    }

    private Deadline scadenzaPerUtente(User user) {
        return Deadline.builder()
                .id(UUID.randomUUID())
                .user(user)
                .descrizione("Presentare dichiarazione dei redditi")
                .dataScadenza(LocalDate.now().plusDays(5))
                .urgenza(UrgenzaScadenza.ALTA)
                .notificata(false)
                .completata(false)
                .build();
    }
}