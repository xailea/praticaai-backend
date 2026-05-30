package it.praticaai.service;

import it.praticaai.config.AppProperties;
import it.praticaai.exception.PlanLimitExceededException;
import it.praticaai.model.Piano;
import it.praticaai.model.User;
import it.praticaai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanService — gestione limiti piano")
class PlanServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AppProperties appProperties;

    @InjectMocks
    PlanService planService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    // ── Piano Free ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Free: sotto limite → nessuna eccezione")
    void free_sottoLimite_ok() {
        User user = utenteFree(2, LocalDate.now().plusMonths(1));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(appProperties.getFreePlanLimit())
                .thenReturn(3);

        assertThatNoException()
                .isThrownBy(() -> planService.checkAndEnforceLimit(userId));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Free: limite raggiunto → PlanLimitExceededException")
    void free_limiteRaggiunto_eccezione() {
        User user = utenteFree(3, LocalDate.now().plusMonths(1));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(appProperties.getFreePlanLimit())
                .thenReturn(3);

        assertThatThrownBy(() -> planService.checkAndEnforceLimit(userId))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("3");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Free: mese scaduto → reset contatore e nessuna eccezione")
    void free_meseScaduto_resetContatore() {
        User user = utenteFree(3, LocalDate.now().minusDays(1));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        when(appProperties.getFreePlanLimit())
                .thenReturn(3);

        assertThatNoException()
                .isThrownBy(() -> planService.checkAndEnforceLimit(userId));

        assertThat(user.getDocumentiMese()).isEqualTo(0);
        assertThat(user.getResetMese()).isAfter(LocalDate.now());

        verify(userRepository).save(user);
    }

    // ── Piano Pro ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Pro: nessun limite applicato")
    void pro_nessunaEccezione() {
        User user = utentePro();
        user.setDocumentiMese(999);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        assertThatNoException()
                .isThrownBy(() -> planService.checkAndEnforceLimit(userId));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(appProperties);
    }

    // ── isFeatureAvailable ─────────────────────────────────────────────────

    @Test
    @DisplayName("Free: funzionalità pro non disponibili")
    void free_featureProNonDisponibili() {
        User user = utenteFree(0, LocalDate.now().plusMonths(1));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        assertThat(planService.isFeatureAvailable(userId, PlanService.Feature.SCADENZE)).isFalse();
        assertThat(planService.isFeatureAvailable(userId, PlanService.Feature.EXPORT_PDF)).isFalse();
        assertThat(planService.isFeatureAvailable(userId, PlanService.Feature.ANALISI_BASE)).isTrue();

        verifyNoInteractions(appProperties);
    }

    @Test
    @DisplayName("Pro: tutte le funzionalità disponibili")
    void pro_tutteLeFeatureDisponibili() {
        User user = utentePro();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        assertThat(planService.isFeatureAvailable(userId, PlanService.Feature.SCADENZE)).isTrue();
        assertThat(planService.isFeatureAvailable(userId, PlanService.Feature.EXPORT_PDF)).isTrue();
        assertThat(planService.isFeatureAvailable(userId, PlanService.Feature.DASHBOARD)).isTrue();

        verifyNoInteractions(appProperties);
    }

    // ── Factory helpers ────────────────────────────────────────────────────

    private User utenteFree(int documentiMese, LocalDate resetMese) {
        return User.builder()
                .id(userId)
                .email("test@praticaai.it")
                .piano(Piano.FREE)
                .documentiMese(documentiMese)
                .resetMese(resetMese)
                .build();
    }

    private User utentePro() {
        return User.builder()
                .id(userId)
                .email("pro@praticaai.it")
                .piano(Piano.PRO)
                .documentiMese(0)
                .resetMese(LocalDate.now().plusMonths(1))
                .build();
    }
}