package it.praticaai.controller;

import it.praticaai.config.AppProperties;
import it.praticaai.controller.dto.UserResponse;
import it.praticaai.model.User;
import it.praticaai.security.JwtClaimsExtractor;
import it.praticaai.security.SecurityUtils;
import it.praticaai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GET /api/me — profilo utente + piano + contatore documenti
 *
 * Questo endpoint fa anche da "init" al primo accesso:
 * getOrCreateUser crea la riga nel DB se non esiste ancora.
 * Il frontend lo chiama subito dopo il login.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService    userService;
    private final AppProperties  appProperties;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(HttpServletRequest request) {
        UUID   userId = SecurityUtils.getAuthUserId();
        String email  = JwtClaimsExtractor.extractEmail(request);

        // Upsert: crea il profilo se è il primo accesso
        User user = userService.getOrCreateUser(userId, email);
        return ResponseEntity.ok(UserResponse.from(user, appProperties));
    }
}
