package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.Role;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    Role findFirstByNameEquals(String name);
}
