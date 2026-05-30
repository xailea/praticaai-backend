package it.praticaai.model;

/**
 * Ciclo di vita di un documento caricato.
 *
 *  PENDING     → appena salvato, in attesa di processazione
 *  PROCESSING  → chiamata a GPT-4o in corso
 *  DONE        → analisi completata e salvata
 *  ERROR       → errore durante l'analisi (dettagli nel log)
 */
public enum StatoDocumento {
    PENDING,
    PROCESSING,
    DONE,
    ERROR
}
