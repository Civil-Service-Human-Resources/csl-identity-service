package uk.gov.cabinetoffice.csl.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Oauth2Authorization;

@Repository
public interface Oauth2AuthorizationRepository extends CrudRepository<Oauth2Authorization, Long> {

    @Transactional
    Long deleteByPrincipalName(String principalName);
}
