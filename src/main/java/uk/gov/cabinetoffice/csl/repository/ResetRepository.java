package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.Reset;

@Repository
public interface ResetRepository extends CrudRepository<Reset, Long> {

    boolean existsByCode(String code);

    Reset findByCode(String code);
}
