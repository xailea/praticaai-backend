package it.praticaai.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Estrae claim aggiuntivi dal JWT senza ri-verificarlo
 * (la verifica è già avvenuta in JwtFilter).
 *
 * Usato da UserController per ottenere l'email al primo login
 * senza un'ulteriore chiamata a Supabase.
 */
public final class JwtClaimsExtractor {

    private JwtClaimsExtractor() {}

    public static String extractEmail(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        // Il JWT è già stato verificato da JwtFilter — decode senza verifica è sicuro
        DecodedJWT jwt = JWT.decode(header.substring(7));
        return jwt.getClaim("email").asString();
    }
}
