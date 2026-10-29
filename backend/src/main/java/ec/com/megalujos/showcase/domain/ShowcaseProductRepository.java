package ec.com.megalujos.showcase.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowcaseProductRepository extends JpaRepository<ShowcaseProduct, ShowcaseProductId> {
    List<ShowcaseProduct> findByIdShowcaseIdOrderBySortOrderAsc(UUID showcaseId);
}
