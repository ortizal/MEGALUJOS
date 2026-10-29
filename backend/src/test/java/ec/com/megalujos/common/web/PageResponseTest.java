package ec.com.megalujos.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de PageResponse: copia exacta de campos desde Page de Spring Data.
 */
class PageResponseTest {

    @Test
    void from_copiaTodosLosCamposDeLaPage() {
        // Given
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7);

        // When
        PageResponse<String> response = PageResponse.from(page);

        // Then
        assertEquals(List.of("a", "b"), response.getItems());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getSize());
        assertEquals(7, response.getTotalElements());
        assertEquals(4, response.getTotalPages());
    }

    @Test
    void from_conMapper_transformaLosItems() {
        // Given
        Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(0, 5), 3);

        // When
        PageResponse<String> response = PageResponse.from(page, n -> "N-" + n);

        // Then
        assertEquals(List.of("N-1", "N-2", "N-3"), response.getItems());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(3, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void from_pageVacia_devuelveItemsVacios() {
        // Given
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        // When
        PageResponse<String> response = PageResponse.from(page);

        // Then
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
    }
}
