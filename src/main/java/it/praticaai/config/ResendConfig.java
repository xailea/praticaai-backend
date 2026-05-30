package it.praticaai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "resend")
public class ResendConfig {

    private String apiKey;
    private String fromAddress = "noreply@praticaai.it";
    private String fromName    = "PraticaAI";

    /** Quanti giorni prima della scadenza inviare il reminder. */
    private int reminderDaysBefore = 7;
}
