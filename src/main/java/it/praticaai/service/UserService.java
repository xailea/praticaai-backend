package it.praticaai.service;

import it.praticaai.model.Piano;
import it.praticaai.model.User;
import it.praticaai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Recupera il profilo utente, creandolo se non esiste ancora.
     *
     * Supabase Auth gestisce la registrazione, ma il nostro DB non ha ancora
     * la riga utente al primo login. Questo metodo fa upsert in modo idempotente:
     * chiamarlo N volte con gli stessi dati produce sempre lo stesso risultato.
     */
    @Transactional
    public User getOrCreateUser(UUID userId, String email) {
        return userRepository.findById(userId).orElseGet(() -> {
            log.info("Primo accesso per userId={}, creo profilo", userId);
            User newUser = User.builder()
                    .id(userId)
                    .email(email)
                    .piano(Piano.FREE)
                    .documentiMese(0)
                    .resetMese(LocalDate.now().plusMonths(1).withDayOfMonth(1))
                    .build();
            return userRepository.save(newUser);
        });
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utente non trovato: " + userId));
    }
}
