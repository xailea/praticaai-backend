package it.praticaai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseConfig {

    private String url;
    private String serviceRoleKey;
    private String jwtSecret;
    private String bucket = "documenti";
}
