package it.praticaai.controller;

import it.praticaai.controller.dto.DeadlineResponse;
import it.praticaai.controller.dto.DeadlineUpdateRequest;
import it.praticaai.security.SecurityUtils;
import it.praticaai.service.DeadlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/deadlines       — lista scadenze attive (non completate)
 * PUT /api/deadlines/{id}  — aggiorna stato (completata / notificata)
 */
@RestController
@RequestMapping("/api/deadlines")
@RequiredArgsConstructor
public class DeadlineController {

    private final DeadlineService deadlineService;

    @GetMapping
    public ResponseEntity<List<DeadlineResponse>> listDeadlines() {
        UUID userId = SecurityUtils.getAuthUserId();
        List<DeadlineResponse> deadlines = deadlineService.listDeadlines(userId)
                .stream()
                .map(DeadlineResponse::from)
                .toList();
        return ResponseEntity.ok(deadlines);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeadlineResponse> updateDeadline(
            @PathVariable UUID id,
            @RequestBody DeadlineUpdateRequest request) {

        UUID userId = SecurityUtils.getAuthUserId();
        return ResponseEntity.ok(
                DeadlineResponse.from(deadlineService.updateDeadline(id, userId, request))
        );
    }
}
