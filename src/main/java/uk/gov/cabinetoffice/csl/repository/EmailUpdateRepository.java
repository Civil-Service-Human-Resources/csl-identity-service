package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailUpdateRepository extends JpaRepository<EmailUpdate, Long> {

    Optional<EmailUpdate> findByCode(String code);

    boolean existsByCode(String code);

    List<EmailUpdate> findByNewEmailIgnoreCaseAndPreviousEmailIgnoreCaseAndEmailUpdateStatus(
            String newEmail, String previousEmail, EmailUpdateStatus emailUpdateStatus);
}
