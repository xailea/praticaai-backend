package it.praticaai.repository;

import it.praticaai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Incrementa il contatore documenti in un'unica query UPDATE,
     * senza dover caricare, modificare e salvare l'entity.
     * Più efficiente di findById + save per un'operazione atomica.
     */
    @Modifying
    @Query("UPDATE User u SET u.documentiMese = u.documentiMese + 1 WHERE u.id = :userId")
    void incrementaDocumentiMese(UUID userId);
}
