package ec.com.megalujos.invoicing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectronicDocumentFileRepository extends JpaRepository<ElectronicDocumentFile, ElectronicDocumentFileId> {
    Optional<ElectronicDocumentFile> findByDocumentIdAndFileType(UUID documentId, String fileType);
}
