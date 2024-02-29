package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.domain.ReactivationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactivationRepository extends CrudRepository<Reactivation, Long> {

    Optional<Reactivation> findFirstByCodeAndReactivationStatusEquals(String code,
                                            ReactivationStatus reactivationStatus);

    List<Reactivation> findByEmailIgnoreCaseAndReactivationStatusEquals(String email,
                                            ReactivationStatus reactivationStatus);
}
