package it.praticaai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO che mappa il JSON strutturato restituito da GPT-4o.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true): se GPT aggiunge campi extra
 * in futuro, non esplode. Robustezza > rigidità.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {

    @JsonProperty("tipo_documento")
    private String tipoDocumento;

    @JsonProperty("titolo_breve")
    private String titoloBreve;

    @JsonProperty("spiegazione")
    private String spiegazione;

    @JsonProperty("punti_chiave")
    private List<String> puntiChiave;

    @JsonProperty("scadenze")
    private List<ScadenzaDto> scadenze;

    @JsonProperty("azioni_consigliate")
    private List<String> azioniConsigliate;

    @JsonProperty("rischi")
    private List<String> rischi;

    @JsonProperty("domande_frequenti")
    private List<DomandaFaq> domandeFrecuenti;

    @JsonProperty("affidabilita")
    private double affidabilita;

    // ── DTO interni ────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScadenzaDto {

        @JsonProperty("descrizione")
        private String descrizione;

        /** Formato YYYY-MM-DD oppure null se la data non è determinabile. */
        @JsonProperty("data")
        private String data;

        /** "alta" | "media" | "bassa" — lowercase come restituito da GPT. */
        @JsonProperty("urgenza")
        private String urgenza;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DomandaFaq {

        @JsonProperty("domanda")
        private String domanda;

        @JsonProperty("risposta")
        private String risposta;
    }
}
