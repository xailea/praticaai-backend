package it.praticaai.controller.dto;

import lombok.Data;

/**
 * Body per PUT /api/deadlines/{id}
 * Entrambi i campi sono opzionali: il controller aggiorna
 * solo quelli presenti (non-null).
 */
@Data
public class DeadlineUpdateRequest {
    private Boolean completata;
    private Boolean notificata;
}
