package it.praticaai.service;

import it.praticaai.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Gestisce upload e download di file su Supabase Storage.
 *
 * Supabase Storage espone un'API REST compatibile con S3.
 * Usiamo OkHttp direttamente: RestClient di Spring 6.1 va bene
 * per JSON, ma per upload multipart binari OkHttp è più esplicito.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final SupabaseConfig supabaseConfig;
    private final OkHttpClient httpClient;

    /**
     * Carica il file nel bucket Supabase e restituisce il path salvato.
     * Path formato: "{userId}/{uuid}-{nomeFile}"
     * Questo garantisce unicità e isolamento per utente.
     */
    public String uploadFile(UUID userId, MultipartFile file) throws IOException {
        String fileName   = sanitizeFileName(file.getOriginalFilename());
        String storagePath = userId + "/" + UUID.randomUUID() + "-" + fileName;
        String uploadUrl   = supabaseConfig.getUrl()
                + "/storage/v1/object/"
                + supabaseConfig.getBucket() + "/"
                + storagePath;

        RequestBody body = RequestBody.create(
                file.getBytes(),
                MediaType.parse(file.getContentType())
        );

        Request request = new Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                .header("x-upsert", "false")   // non sovrascrivere file esistenti
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new IOException("Upload Supabase fallito [" + response.code() + "]: " + errorBody);
            }
        }

        log.info("File caricato su Supabase: {}", storagePath);
        return storagePath;
    }

    /**
     * Scarica il file dallo storage e restituisce i bytes.
     * Usato da OpenAIService prima di passare il file a GPT-4o.
     */
    public byte[] downloadFile(String storagePath) throws IOException {
        String downloadUrl = supabaseConfig.getUrl()
                + "/storage/v1/object/"
                + supabaseConfig.getBucket() + "/"
                + storagePath;

        Request request = new Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download Supabase fallito [" + response.code() + "]: " + storagePath);
            }
            if (response.body() == null) {
                throw new IOException("Body vuoto per: " + storagePath);
            }
            return response.body().bytes();
        }
    }

    /**
     * Elimina il file dallo storage.
     * Chiamato da DocumentService quando l'utente cancella un documento.
     */
    public void deleteFile(String storagePath) {
        String deleteUrl = supabaseConfig.getUrl()
                + "/storage/v1/object/"
                + supabaseConfig.getBucket() + "/"
                + storagePath;

        Request request = new Request.Builder()
                .url(deleteUrl)
                .header("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Non lanciamo eccezione: il documento DB viene cancellato comunque.
                // Lo storage può avere file orfani che puliamo in batch separato.
                log.warn("Eliminazione file Supabase fallita [{}]: {}", response.code(), storagePath);
            } else {
                log.info("File eliminato da Supabase: {}", storagePath);
            }
        } catch (IOException e) {
            log.warn("Errore di rete durante eliminazione file: {}", storagePath, e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String sanitizeFileName(String originalName) {
        if (originalName == null) return "documento";
        // Rimuove caratteri non sicuri per URL/path
        return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
