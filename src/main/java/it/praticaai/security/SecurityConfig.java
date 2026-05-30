package it.praticaai.security;

import it.praticaai.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configurazione Spring Security.
 *
 * Scelte architetturali:
 * - Stateless: nessuna sessione HTTP, ogni request porta il suo JWT
 * - CSRF disabilitato: non necessario per API REST stateless con JWT
 * - CORS configurato da application.yml tramite AppProperties
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // abilita @PreAuthorize sui controller se serve
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Nessuna sessione: REST + JWT è stateless per definizione
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF inutile senza cookie di sessione
            .csrf(AbstractHttpConfigurer::disable)

            // CORS: origini lette da application.yml
            .cors(c -> c.configurationSource(corsConfigurationSource()))

            // Regole di accesso
            .authorizeHttpRequests(auth -> auth
                // Endpoint pubblici
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                // Tutto il resto richiede autenticazione
                .anyRequest().authenticated()
            )

            // Inserisce il nostro filtro JWT prima del filtro standard di Spring
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origini da application.yml (localhost:4200 in dev, dominio prod)
        config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Necessario per Authorization header e Content-Type
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Espone header utili al frontend (es. X-Total-Count per paginazione)
        config.setExposedHeaders(List.of("X-Total-Count"));

        // Permette credenziali (cookie, Authorization header)
        config.setAllowCredentials(true);

        // Cache preflight per 1 ora: riduce richieste OPTIONS
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
