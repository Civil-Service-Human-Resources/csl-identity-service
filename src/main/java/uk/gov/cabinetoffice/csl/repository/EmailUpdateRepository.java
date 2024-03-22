package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;

import java.util.Optional;

@Repository
public interface EmailUpdateRepository extends JpaRepository<EmailUpdate, Long> {

    Optional<EmailUpdate> findByCode(String code);

    boolean existsByCode(String code);
}
