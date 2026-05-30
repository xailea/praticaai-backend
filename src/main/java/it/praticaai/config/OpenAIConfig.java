package it.praticaai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAIConfig {

    private String apiKey;
    private String model = "gpt-4o";
    private int maxTokens = 2000;
    private String baseUrl = "https://api.openai.com/v1";
}
