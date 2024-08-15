package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.InviteStatus;

import java.util.Optional;

@Repository
public interface InviteRepository extends CrudRepository<Invite, Long> {

    Invite findByForEmailIgnoreCase(String forEmail);

    Invite findByCode(String code);

    Optional<Invite> findByForEmailIgnoreCaseAndStatus(String email, InviteStatus status);

    boolean existsByCode(String code);

    boolean existsByForEmailIgnoreCaseAndStatus(String email, InviteStatus status);

    boolean existsByForEmailIgnoreCaseAndInviterIdIsNotNull(String email);
}
