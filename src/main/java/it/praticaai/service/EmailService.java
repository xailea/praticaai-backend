package it.praticaai.service;

import it.praticaai.config.ResendConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Invia email transazionali tramite Resend.com.
 *
 * Resend espone una REST API semplice: POST /emails con JSON.
 * Non c'è un SDK Java ufficiale stabile, quindi chiamiamo
 * l'API direttamente con OkHttp — lo stesso client già configurato.
 *
 * Documentazione: https://resend.com/docs/api-reference/emails/send-email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final ResendConfig  resendConfig;
    private final OkHttpClient  httpClient;
    private final ObjectMapper  objectMapper;

    /**
     * Invia una email HTML.
     *
     * @param to          indirizzo destinatario
     * @param subject     oggetto email
     * @param htmlBody    corpo HTML
     */
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "from",    resendConfig.getFromName() + " <" + resendConfig.getFromAddress() + ">",
                    "to",      new String[]{to},
                    "subject", subject,
                    "html",    htmlBody
            ));

            RequestBody body = RequestBody.create(payload, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(RESEND_API_URL)
                    .header("Authorization", "Bearer " + resendConfig.getApiKey())
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    log.error("Resend API error [{}] per {}: {}", response.code(), to, errorBody);
                } else {
                    log.info("Email inviata a {} — oggetto: '{}'", to, subject);
                }
            }
        } catch (IOException e) {
            // Non propaghiamo: un'email fallita non deve bloccare il flusso applicativo
            log.error("Errore invio email a {}: {}", to, e.getMessage(), e);
        }
    }
}
