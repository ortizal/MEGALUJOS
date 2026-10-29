package ec.com.megalujos.showcase.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    @Query("""
            SELECT p FROM Promotion p
            WHERE p.active = true AND p.startsAt <= :now AND p.endsAt >= :now
            ORDER BY p.startsAt DESC
            """)
    List<Promotion> findCurrentPromotions(@Param("now") OffsetDateTime now);
}
