package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

@Slf4j
@AllArgsConstructor
@Service
public class AgencyTokenCapacityService {

    private IdentityRepository identityRepository;

    public boolean hasSpaceAvailable(AgencyToken agencyToken) {
        Long spacesUsed = identityRepository.countByAgencyTokenUid(agencyToken.getUid());

        log.debug("Agency token uid={}, capacity={}, spaces used={}", agencyToken.getUid(), agencyToken.getCapacity(), spacesUsed);

        return (agencyToken.getCapacity() - spacesUsed) > 0;
    }

    public AgencyToken getSpacesUsedByAgencyToken(String uid) {
        return new AgencyToken(identityRepository.countByAgencyTokenUid(uid));
    }

    public Long getCountOfAgencyByUid(String uid) {
        return identityRepository.countByAgencyTokenUid(uid);
    }

    public void deleteAgencyToken(String agencyTokenUid) {
        identityRepository.removeAgencyToken(agencyTokenUid);
    }
}
