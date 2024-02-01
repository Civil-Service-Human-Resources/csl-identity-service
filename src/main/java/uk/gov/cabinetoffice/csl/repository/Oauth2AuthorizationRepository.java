package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.domain.Oauth2Authorization;

@Repository
public interface Oauth2AuthorizationRepository extends CrudRepository<Oauth2Authorization, Long> {

    long deleteByPrincipalName(String principalName);
}
