package it.praticaai.model;

/**
 * Piano di abbonamento dell'utente.
 * Mappato come stringa nel DB (EnumType.STRING) per leggibilità e sicurezza
 * in caso di riordinamento dei valori.
 */
public enum Piano {
    FREE,
    PRO,
    FAMIGLIA
}
