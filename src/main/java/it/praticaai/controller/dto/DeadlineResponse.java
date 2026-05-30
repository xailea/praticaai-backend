package it.praticaai.controller.dto;

import it.praticaai.model.Deadline;
import it.praticaai.model.UrgenzaScadenza;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class DeadlineResponse {

    UUID            id;
    UUID            documentId;
    String          descrizione;
    LocalDate       dataScadenza;
    UrgenzaScadenza urgenza;
    boolean         notificata;
    boolean         completata;

    public static DeadlineResponse from(Deadline d) {
        return DeadlineResponse.builder()
                .id(d.getId())
                .documentId(d.getDocument() != null ? d.getDocument().getId() : null)
                .descrizione(d.getDescrizione())
                .dataScadenza(d.getDataScadenza())
                .urgenza(d.getUrgenza())
                .notificata(d.isNotificata())
                .completata(d.isCompletata())
                .build();
    }
}
