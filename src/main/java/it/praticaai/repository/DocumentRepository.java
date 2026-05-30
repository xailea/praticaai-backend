package it.praticaai.repository;

import it.praticaai.model.Document;
import it.praticaai.model.StatoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Lista paginata dei documenti dell'utente, ordinati per data discendente.
     * La paginazione è fondamentale: un utente Pro con storico lungo
     * non deve scaricare tutto in una volta.
     */
    Page<Document> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Recupero sicuro: verifica contestualmente che il documento
     * appartenga all'utente. Previene accessi cross-user senza
     * dover fare due query separate.
     */
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    /** Conta i documenti con un certo stato — utile per monitoring. */
    long countByUserIdAndStato(UUID userId, StatoDocumento stato);

    /**
     * Verifica esistenza per hash-based caching (Modulo 3).
     * Se lo stesso file viene caricato due volte, riusiamo l'analisi esistente.
     */
    @Query("SELECT d FROM Document d WHERE d.userId = :userId AND d.storagePath = :path")
    Optional<Document> findByUserIdAndStoragePath(UUID userId, String path);
}
