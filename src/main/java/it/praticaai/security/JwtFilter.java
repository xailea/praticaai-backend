package it.praticaai.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import it.praticaai.config.SupabaseConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro JWT per Supabase.
 *
 * Flusso:
 *  1. Legge "Authorization: Bearer <token>" dall'header
 *  2. Verifica firma HMAC-SHA256 con SUPABASE_JWT_SECRET
 *  3. Estrae sub (UUID utente) e role dal payload
 *  4. Popola il SecurityContext — da quel momento @AuthenticationPrincipal funziona
 *
 * Se il token manca o è invalido, la richiesta prosegue senza autenticazione
 * e sarà bloccata da SecurityConfig sugli endpoint protetti.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final SupabaseConfig supabaseConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                DecodedJWT jwt = verifyToken(token);
                setAuthentication(jwt);
            } catch (JWTVerificationException e) {
                log.warn("JWT non valido [{}]: {}", request.getRequestURI(), e.getMessage());
                // Non blocchiamo qui: SecurityConfig si occupa di rifiutare le richieste
                // su endpoint protetti. Questo permette gli endpoint pubblici (/api/health).
            }
        }

        chain.doFilter(request, response);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(supabaseConfig.getJwtSecret());
        return JWT.require(algorithm)
                .withIssuer("supabase")   // Supabase emette con iss = "supabase"
                .build()
                .verify(token);
    }

    private void setAuthentication(DecodedJWT jwt) {
        String userId = jwt.getSubject();       // UUID utente in Supabase Auth
        String role   = jwt.getClaim("role").asString();  // "authenticated" o "anon"

        // Usiamo userId come principal: i service layer lo estraggono con getAuthUserId()
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.fromString(userId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Autenticato userId={} role={}", userId, role);
    }
}
