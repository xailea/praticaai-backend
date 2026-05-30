package it.praticaai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Legge il blocco "app:" da application.yml e lo espone come bean tipizzato.
 * Preferibile a @Value sparsi: tutto in un posto, testabile, refactor-safe.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Numero massimo di documenti analizzabili al mese per il piano Free. */
    private int freePlanLimit = 3;

    /** Dimensione massima file in MB. */
    private int maxFileSizeMb = 10;

    /** MIME type accettati in upload. */
    private List<String> allowedMimeTypes = List.of(
            "application/pdf", "image/jpeg", "image/png"
    );

    /** Configurazione CORS. */
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");
    }
}
