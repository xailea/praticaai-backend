package it.praticaai.service;

import it.praticaai.controller.dto.DeadlineUpdateRequest;
import it.praticaai.exception.ResourceNotFoundException;
import it.praticaai.model.Deadline;
import it.praticaai.repository.DeadlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;

    public List<Deadline> listDeadlines(UUID userId) {
        return deadlineRepository.findByUserIdAndCompletataFalseOrderByDataScadenzaAsc(userId);
    }

    @Transactional
    public Deadline updateDeadline(UUID deadlineId, UUID userId, DeadlineUpdateRequest request) {
        Deadline deadline = deadlineRepository.findByIdAndUserId(deadlineId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline", deadlineId));

        // Aggiorna solo i campi presenti nella request (patch semantics)
        if (request.getCompletata() != null) {
            deadline.setCompletata(request.getCompletata());
        }
        if (request.getNotificata() != null) {
            deadline.setNotificata(request.getNotificata());
        }

        return deadlineRepository.save(deadline);
    }
}
