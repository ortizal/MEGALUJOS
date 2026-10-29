package ec.com.megalujos.invoicing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElectronicDocumentEventRepository extends JpaRepository<ElectronicDocumentEvent, UUID> {
    List<ElectronicDocumentEvent> findByDocumentIdOrderByOccurredAtAsc(UUID documentId);
}
