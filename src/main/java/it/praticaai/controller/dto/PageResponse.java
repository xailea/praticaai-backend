package it.praticaai.controller.dto;

import lombok.Value;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Wrapper generico per risposte paginate.
 *
 * Invece di esporre direttamente il Page<T> di Spring (che porta
 * dettagli interni di Hibernate), restituiamo un JSON pulito:
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 42,
 *   "totalPages": 3,
 *   "last": false
 * }
 *
 * Uso: PageResponse.of(page, DocumentResponse::from)
 */
@Value
public class PageResponse<T> {

    List<T> content;
    int     page;
    int     size;
    long    totalElements;
    int     totalPages;
    boolean last;

    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
