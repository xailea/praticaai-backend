package it.praticaai.repository;

import it.praticaai.model.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, UUID> {

    /**
     * Scadenze attive dell'utente (non completate), ordinate per data.
     * La dashboard mostra sempre le prossime scadenze per prime.
     */
    List<Deadline> findByUserIdAndCompletataFalseOrderByDataScadenzaAsc(UUID userId);

    /** Accesso sicuro cross-user — stesso pattern di DocumentRepository. */
    Optional<Deadline> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Query usata dallo scheduler giornaliero (Modulo 5).
     * Trova le scadenze da notificare:
     * - non ancora notificate
     * - non completate
     * - con data scadenza entro X giorni da oggi
     */
    @Query("""
            SELECT d FROM Deadline d
            WHERE d.notificata = false
              AND d.completata = false
              AND d.dataScadenza IS NOT NULL
              AND d.dataScadenza <= :entroData
            """)
    List<Deadline> findScadenzeToNotify(LocalDate entroData);

    /** Scadenze legate a un documento specifico. */
    List<Deadline> findByDocumentId(UUID documentId);
}
