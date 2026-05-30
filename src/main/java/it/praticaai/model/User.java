package it.praticaai.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Rappresenta un utente registrato.
 *
 * L'id è l'UUID emesso da Supabase Auth: non lo generiamo noi,
 * lo riceviamo dal JWT e lo usiamo come PK.
 * Questo mantiene la sincronizzazione perfetta tra Supabase e il nostro DB.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * UUID fornito da Supabase Auth — non auto-generato.
     * Corrisponde al campo "sub" del JWT.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Piano attivo. DEFAULT 'free' gestito a livello applicativo (@Builder.Default)
     * e a livello DB tramite DEFAULT nella migration SQL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "piano", nullable = false, length = 20)
    @Builder.Default
    private Piano piano = Piano.FREE;

    /**
     * Contatore documenti analizzati nel mese corrente.
     * Viene azzerato quando LocalDate.now() supera resetMese.
     */
    @Column(name = "documenti_mese", nullable = false)
    @Builder.Default
    private int documentiMese = 0;

    /**
     * Data entro cui il contatore verrà azzerato (= primo giorno del mese successivo).
     */
    @Column(name = "reset_mese", nullable = false)
    private LocalDate resetMese;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (resetMese == null) {
            resetMese = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        }
    }
}
