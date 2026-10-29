package ec.com.megalujos.identity.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u " +
           "WHERE (CAST(:q AS STRING) IS NULL OR u.username ILIKE CONCAT('%', CAST(:q AS STRING), '%') " +
           "OR u.fullName ILIKE CONCAT('%', CAST(:q AS STRING), '%') " +
           "OR u.email ILIKE CONCAT('%', CAST(:q AS STRING), '%')) " +
           "AND (CAST(:active AS STRING) IS NULL OR u.active = :active) " +
           "ORDER BY u.username")
    Page<User> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
