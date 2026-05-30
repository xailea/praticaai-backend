package it.praticaai.controller;

import it.praticaai.controller.dto.DocumentResponse;
import it.praticaai.controller.dto.PageResponse;
import it.praticaai.model.Document;
import it.praticaai.security.SecurityUtils;
import it.praticaai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Endpoints documenti.
 *
 * POST /api/documents/upload  — carica e analizza documento
 * GET  /api/documents          — lista paginata
 * GET  /api/documents/{id}     — dettaglio completo con analisi AI
 * DELETE /api/documents/{id}   — eliminazione
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload e analisi documento.
     *
     * Riceve il file come multipart/form-data.
     * L'analisi GPT-4o avviene in modo sincrono: la risposta arriva
     * quando l'analisi è completa (~10-30 secondi).
     * In Fase 2 si può spostare su coda asincrona se necessario.
     *
     * Risponde HTTP 202 Accepted per indicare che il processing
     * è avvenuto con successo ma potrebbe ancora essere in corso.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) throws IOException {

        UUID userId = SecurityUtils.getAuthUserId();
        log.info("Upload documento: userId={} file={} size={}",
                userId, file.getOriginalFilename(), file.getSize());

        Document document = documentService.uploadAndAnalyze(userId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(DocumentResponse.from(document));
    }

    /**
     * Lista paginata dei documenti dell'utente.
     *
     * Query params:
     *   page  — numero pagina (0-based, default 0)
     *   size  — elementi per pagina (default 20, max 50)
     *
     * Restituisce DocumentResponse.summary (senza analisiJson)
     * per tenere le risposte di lista leggere.
     */
    @GetMapping
    public ResponseEntity<PageResponse<DocumentResponse>> listDocuments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Cap a 50 per evitare richieste abusive
        size = Math.min(size, 50);

        UUID userId = SecurityUtils.getAuthUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documents = documentService.listDocuments(userId, pageable);

        return ResponseEntity.ok(PageResponse.of(documents, DocumentResponse::summary));
    }

    /**
     * Dettaglio documento con analisi AI completa.
     * Include analisiJson — usato dalla schermata di dettaglio del frontend.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getAuthUserId();
        Document document = documentService.getDocument(id, userId);
        return ResponseEntity.ok(DocumentResponse.from(document));
    }

    /**
     * Elimina documento, file dallo storage e scadenze correlate.
     * HTTP 204 No Content — nessun body nella risposta.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getAuthUserId();
        documentService.deleteDocument(id, userId);
        return ResponseEntity.noContent().build();
    }
}
