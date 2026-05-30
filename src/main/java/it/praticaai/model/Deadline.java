package it.praticaai.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Scadenza estratta dall'analisi AI di un documento.
 *
 * Ogni documento può generare N scadenze.
 * Lo scheduler (Modulo 5) interroga questa tabella ogni giorno
 * per inviare reminder email agli utenti Pro.
 */
@Entity
@Table(name = "deadlines", indexes = {
        @Index(name = "idx_deadlines_user_id", columnList = "user_id"),
        // Lo scheduler filtra per data e notificata=false
        @Index(name = "idx_deadlines_data_notificata", columnList = "data_scadenza, notificata")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * Documento da cui è stata estratta la scadenza.
     * Nullable: in futuro si potranno creare scadenze manuali.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", updatable = false)
    private Document document;

    @Column(name = "descrizione", nullable = false)
    private String descrizione;

    /** Null se GPT-4o non ha trovato una data precisa nel documento. */
    @Column(name = "data_scadenza")
    private LocalDate dataScadenza;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgenza", nullable = false, length = 10)
    @Builder.Default
    private UrgenzaScadenza urgenza = UrgenzaScadenza.MEDIA;

    /** true = email reminder già inviata. Evita duplicati nello scheduler. */
    @Column(name = "notificata", nullable = false)
    @Builder.Default
    private boolean notificata = false;

    /** true = l'utente ha segnato la scadenza come completata. */
    @Column(name = "completata", nullable = false)
    @Builder.Default
    private boolean completata = false;
}
