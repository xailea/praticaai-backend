package it.praticaai.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Bean OkHttpClient condiviso.
 *
 * Un singolo client condiviso è la best practice OkHttp:
 * gestisce internamente il connection pool e il thread pool.
 * Creare un'istanza per ogni chiamata è un anti-pattern.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)   // GPT-4o Vision può impiegare fino a 30-40s
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
