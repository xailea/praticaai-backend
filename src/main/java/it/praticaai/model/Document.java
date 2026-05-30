package it.praticaai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Documento caricato dall'utente e analizzato da GPT-4o.
 *
 * Il campo analisiJson è JSONB in PostgreSQL: permette query sui campi
 * interni (es. tipo_documento, scadenze) senza dover deserializzare in Java.
 * In Hibernate 6 si usa @JdbcTypeCode(SqlTypes.JSON).
 */
@Entity
@Table(name = "documents", indexes = {
        // Indice sulla FK più usata: lista documenti dell'utente
        @Index(name = "idx_documents_user_id", columnList = "user_id"),
        // Indice sullo stato: utile per query "tutti i PENDING da processare"
        @Index(name = "idx_documents_stato", columnList = "stato")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Relazione verso l'utente proprietario.
     * LAZY: non carichiamo l'utente ogni volta che leggiamo un documento.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "nome_file", nullable = false, length = 255)
    private String nomeFile;

    /** Tipo riconosciuto da GPT-4o: "CUD", "730", "contratto", ecc. Null finché non analizzato. */
    @Column(name = "tipo_documento", length = 100)
    private String tipoDocumento;

    /** Path nel bucket Supabase Storage, es. "userId/uuid-filename.pdf" */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    /**
     * Output JSON strutturato di GPT-4o salvato come JSONB.
     * Hibernate 6 gestisce la serializzazione/deserializzazione automaticamente
     * verso String; in alternativa si può usare Map<String,Object>.
     * Scegliamo String per semplicità: il frontend riceve questo campo as-is.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analisi_json", columnDefinition = "jsonb")
    private String analisiJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    @Builder.Default
    private StatoDocumento stato = StatoDocumento.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
