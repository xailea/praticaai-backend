package it.praticaai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.praticaai.config.OpenAIConfig;
import it.praticaai.model.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Chiama OpenAI GPT-4o Vision per analizzare un documento.
 *
 * Flusso:
 *  1. Carica il system prompt da classpath (prompts/system_prompt.txt)
 *  2. Codifica il file in Base64
 *  3. Invia la request a /v1/messages con image_url data-URI
 *  4. Fa il parse del JSON strutturato nella risposta
 *  5. Restituisce AnalysisResult (DTO tipizzato)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Cache del prompt: lo leggiamo una volta sola all'avvio
    private String cachedSystemPrompt;

    /**
     * Punto di ingresso principale.
     *
     * @param fileBytes  byte[] del documento (PDF o immagine)
     * @param mimeType   es. "application/pdf", "image/jpeg"
     * @return AnalysisResult deserializzato dal JSON di GPT-4o
     */
    public AnalysisResult analyzeDocument(byte[] fileBytes, String mimeType) throws IOException {
        String base64File    = Base64.getEncoder().encodeToString(fileBytes);
        String systemPrompt  = loadSystemPrompt();
        String requestJson   = buildRequestJson(base64File, mimeType, systemPrompt);

        log.debug("Chiamata OpenAI GPT-4o Vision, file size: {} bytes", fileBytes.length);

        String rawResponse = callOpenAI(requestJson);
        String jsonContent = extractContentFromResponse(rawResponse);

        log.debug("Risposta GPT-4o ricevuta, parsing JSON...");

        return objectMapper.readValue(jsonContent, AnalysisResult.class);
    }

    // ── Costruzione request ───────────────────────────────────────────────────

    private String buildRequestJson(String base64File, String mimeType, String systemPrompt)
            throws IOException {

        // Struttura OpenAI Chat Completions con Vision
        Map<String, Object> requestBody = Map.of(
                "model", openAIConfig.getModel(),
                "max_tokens", openAIConfig.getMaxTokens(),
                "messages", List.of(
                        // System message: istruisce GPT sul formato di risposta
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        // User message: il documento come data-URI
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "image_url",
                                                "image_url", Map.of(
                                                        "url", "data:" + mimeType + ";base64," + base64File,
                                                        "detail", "high"   // alta risoluzione per testi piccoli
                                                )
                                        ),
                                        Map.of(
                                                "type", "text",
                                                "text", "Analizza questo documento e restituisci il JSON richiesto."
                                        )
                                )
                        )
                )
        );

        return objectMapper.writeValueAsString(requestBody);
    }

    // ── Chiamata HTTP ─────────────────────────────────────────────────────────

    private String callOpenAI(String requestJson) throws IOException {
        RequestBody body = RequestBody.create(requestJson, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(openAIConfig.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + openAIConfig.getApiKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new IOException("OpenAI API error [" + response.code() + "]: " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Risposta OpenAI vuota");
            }
            return response.body().string();
        }
    }

    // ── Parsing risposta ──────────────────────────────────────────────────────

    /**
     * Estrae il testo dal campo choices[0].message.content della risposta OpenAI.
     * GPT-4o dovrebbe restituire JSON puro (per il system prompt), ma puliamo
     * eventuali backtick markdown per sicurezza.
     */
    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(String rawResponse) throws IOException {
        Map<String, Object> responseMap = objectMapper.readValue(rawResponse, Map.class);

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseMap.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new IOException("Risposta OpenAI senza choices: " + rawResponse);
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        if (content == null || content.isBlank()) {
            throw new IOException("Content GPT-4o vuoto");
        }

        // Rimuove eventuali ```json ... ``` che GPT a volte aggiunge nonostante le istruzioni
        return content
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
    }

    // ── System prompt ─────────────────────────────────────────────────────────

    /**
     * Legge il system prompt da classpath una sola volta.
     * Il file è in src/main/resources/prompts/system_prompt.txt.
     * La cache evita I/O ripetuto su ogni analisi.
     */
    private String loadSystemPrompt() throws IOException {
        if (cachedSystemPrompt == null) {
            ClassPathResource resource = new ClassPathResource("prompts/system_prompt.txt");
            cachedSystemPrompt = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("System prompt caricato ({} chars)", cachedSystemPrompt.length());
        }
        return cachedSystemPrompt;
    }
}
