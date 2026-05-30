package it.praticaai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // attiva @Scheduled per il controllo scadenze (Modulo 5)
public class PraticaAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PraticaAiApplication.class, args);
    }
}
