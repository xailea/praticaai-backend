package it.praticaai.controller.dto;

import it.praticaai.config.AppProperties;
import it.praticaai.model.Piano;
import it.praticaai.model.User;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class UserResponse {

    UUID      id;
    String    email;
    Piano     piano;
    int       documentiMese;
    int       limiteMensile;       // utile al frontend per mostrare "2/3 documenti usati"
    LocalDate resetMese;

    public static UserResponse from(User user, AppProperties appProperties) {
        int limite = user.getPiano() == Piano.FREE
                ? appProperties.getFreePlanLimit()
                : -1;   // -1 = illimitato per Pro/Famiglia

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .piano(user.getPiano())
                .documentiMese(user.getDocumentiMese())
                .limiteMensile(limite)
                .resetMese(user.getResetMese())
                .build();
    }
}
