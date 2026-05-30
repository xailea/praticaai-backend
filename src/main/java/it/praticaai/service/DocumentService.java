package it.praticaai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.praticaai.config.AppProperties;
import it.praticaai.exception.FileTypeNotAllowedException;
import it.praticaai.exception.ResourceNotFoundException;
import it.praticaai.model.*;
import it.praticaai.repository.DeadlineRepository;
import it.praticaai.repository.DocumentRepository;
import it.praticaai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Orchestratore principale del flusso documenti.
 *
 * Sequenza di uploadAndAnalyze:
 *  1. Valida tipo file
 *  2. Controlla limite piano Free (PlanService)
 *  3. Carica file su Supabase Storage
 *  4. Salva Document con stato PENDING
 *  5. Chiama GPT-4o Vision (OpenAIService)
 *  6. Persiste il JSON di analisi, aggiorna stato → DONE
 *  7. Estrae e salva le scadenze in tabella deadlines
 *  8. Incrementa il contatore mensile dell'utente
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository   documentRepository;
    private final UserRepository       userRepository;
    private final DeadlineRepository   deadlineRepository;
    private final StorageService       storageService;
    private final OpenAIService        openAIService;
    private final PlanService          planService;
    private final AppProperties        appProperties;
    private final ObjectMapper         objectMapper;

    // ── Upload + Analisi ──────────────────────────────────────────────────────

    @Transactional
    public Document uploadAndAnalyze(UUID userId, MultipartFile file) throws IOException {

        // 1. Valida MIME type
        validateFileType(file);

        // 2. Controlla limite piano (lancia eccezione se superato)
        planService.checkAndEnforceLimit(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utente non trovato: " + userId));

        // 3. Upload su Supabase Storage
        String storagePath = storageService.uploadFile(userId, file);
        log.info("File caricato: {} -> {}", file.getOriginalFilename(), storagePath);

        // 4. Salva documento in stato PENDING — visibile subito all'utente
        Document document = documentRepository.save(Document.builder()
                .user(user)
                .nomeFile(file.getOriginalFilename())
                .storagePath(storagePath)
                .stato(StatoDocumento.PENDING)
                .build());

        // 5 & 6. Analisi AI (può richiedere 10-30 secondi)
        document = analyzeAndPersist(document, file.getBytes(), file.getContentType());

        // 7. Estrai scadenze dal risultato AI e salvale
        if (document.getStato() == StatoDocumento.DONE) {
            extractAndSaveDeadlines(document, user);
        }

        // 8. Incrementa contatore documenti mensili (solo se analisi riuscita)
        if (document.getStato() == StatoDocumento.DONE) {
            userRepository.incrementaDocumentiMese(userId);
        }

        return document;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public Page<Document> listDocuments(UUID userId, Pageable pageable) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Document getDocument(UUID documentId, UUID userId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
    }

    // ── Eliminazione ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = getDocument(documentId, userId);

        // Elimina file dallo storage (best effort, non blocca se fallisce)
        storageService.deleteFile(document.getStoragePath());

        // Le scadenze vengono eliminate per CASCADE (ON DELETE SET NULL document_id)
        // ma vogliamo eliminarle esplicitamente per pulizia
        deadlineRepository.findByDocumentId(documentId)
                .forEach(deadlineRepository::delete);

        documentRepository.delete(document);
        log.info("Documento eliminato: {} (userId={})", documentId, userId);
    }

    // ── Helpers privati ───────────────────────────────────────────────────────

    /**
     * Chiama GPT-4o, salva il risultato e aggiorna lo stato del documento.
     * In caso di errore AI, aggiorna lo stato a ERROR senza propagare
     * l'eccezione al chiamante — il documento rimane accessibile.
     */
    private Document analyzeAndPersist(Document document, byte[] fileBytes, String mimeType) {
        document.setStato(StatoDocumento.PROCESSING);
        documentRepository.save(document);

        try {
            AnalysisResult result = openAIService.analyzeDocument(fileBytes, mimeType);

            document.setTipoDocumento(result.getTipoDocumento());
            document.setAnalisiJson(objectMapper.writeValueAsString(result));
            document.setStato(StatoDocumento.DONE);

            log.info("Analisi completata: doc={} tipo={} affidabilita={}",
                    document.getId(), result.getTipoDocumento(), result.getAffidabilita());

        } catch (Exception e) {
            log.error("Errore analisi documento {}: {}", document.getId(), e.getMessage(), e);
            document.setStato(StatoDocumento.ERROR);
        }

        return documentRepository.save(document);
    }

    /**
     * Legge le scadenze dall'AnalysisResult e le persiste in tabella deadlines.
     * Converte le stringhe urgenza lowercase ("alta") in enum (ALTA).
     */
    private void extractAndSaveDeadlines(Document document, User user) {
        try {
            AnalysisResult result = objectMapper.readValue(document.getAnalisiJson(), AnalysisResult.class);

            if (result.getScadenze() == null || result.getScadenze().isEmpty()) {
                return;
            }

            List<Deadline> deadlines = result.getScadenze().stream()
                    .map(s -> Deadline.builder()
                            .user(user)
                            .document(document)
                            .descrizione(s.getDescrizione())
                            .dataScadenza(parseDate(s.getData()))
                            .urgenza(parseUrgenza(s.getUrgenza()))
                            .build())
                    .toList();

            deadlineRepository.saveAll(deadlines);
            log.info("Salvate {} scadenze per documento {}", deadlines.size(), document.getId());

        } catch (Exception e) {
            // Non bloccare il flusso se l'estrazione scadenze fallisce
            log.warn("Errore estrazione scadenze per documento {}: {}", document.getId(), e.getMessage());
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Data scadenza non parsabile: '{}'", dateStr);
            return null;
        }
    }

    private UrgenzaScadenza parseUrgenza(String urgenza) {
        if (urgenza == null) return UrgenzaScadenza.MEDIA;
        return switch (urgenza.toLowerCase()) {
            case "alta"  -> UrgenzaScadenza.ALTA;
            case "bassa" -> UrgenzaScadenza.BASSA;
            default      -> UrgenzaScadenza.MEDIA;
        };
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !appProperties.getAllowedMimeTypes().contains(contentType)) {
            throw new FileTypeNotAllowedException(contentType);
        }
    }
}
