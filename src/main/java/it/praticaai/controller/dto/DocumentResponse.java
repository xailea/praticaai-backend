package it.praticaai.controller.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import it.praticaai.model.Document;
import it.praticaai.model.StatoDocumento;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO di risposta per un documento.
 *
 * Usiamo @Value (Lombok) = classe immutabile: tutti i campi final,
 * nessun setter. I DTO di risposta non devono mai essere mutabili.
 *
 * @JsonRawValue su analisiJson: serializza il campo come JSON inline
 * invece di stringa escaped. Il frontend riceve:
 *   "analisiJson": { "tipo_documento": "CUD", ... }
 * invece di:
 *   "analisiJson": "{\"tipo_documento\": \"CUD\", ...}"
 */
@Value
@Builder
public class DocumentResponse {

    UUID          id;
    String        nomeFile;
    String        tipoDocumento;
    StatoDocumento stato;
    Instant       createdAt;

    @JsonRawValue
    String analisiJson;

    /** Factory method: converte l'entity in DTO. */
    public static DocumentResponse from(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .nomeFile(doc.getNomeFile())
                .tipoDocumento(doc.getTipoDocumento())
                .stato(doc.getStato())
                .createdAt(doc.getCreatedAt())
                .analisiJson(doc.getAnalisiJson())
                .build();
    }

    /** Versione compatta per le liste: esclude analisiJson (pesante). */
    public static DocumentResponse summary(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .nomeFile(doc.getNomeFile())
                .tipoDocumento(doc.getTipoDocumento())
                .stato(doc.getStato())
                .createdAt(doc.getCreatedAt())
                .analisiJson(null)
                .build();
    }
}
