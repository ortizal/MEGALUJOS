package ec.com.megalujos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    Optional<MediaFile> findByStorageKey(String storageKey);

    Optional<MediaFile> findBySha256AndOriginalName(String sha256, String originalName);
}
