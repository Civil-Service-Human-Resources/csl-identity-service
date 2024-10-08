package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AgencyTokenCapacityServiceTest {

    private static final String UID = "UID";

    @Mock
    private IdentityRepository identityRepository;

    @InjectMocks
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @Test
    public void shouldReturnTrueIfManySpacesAvailable() {
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);
        agencyToken.setCapacity(100L);

        when(identityRepository.countByAgencyTokenUid(UID)).thenReturn(1L);

        assertTrue(agencyTokenCapacityService.hasSpaceAvailable(agencyToken));
    }

    @Test
    public void shouldReturnTrueIfOneSpaceAvailable() {
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);
        agencyToken.setCapacity(100L);

        when(identityRepository.countByAgencyTokenUid(UID)).thenReturn(99L);

        assertTrue(agencyTokenCapacityService.hasSpaceAvailable(agencyToken));
    }

    @Test
    public void shouldReturnFalseIfNoSpaceAvailable() {
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);
        agencyToken.setCapacity(100L);

        when(identityRepository.countByAgencyTokenUid(UID)).thenReturn(100L);

        assertFalse(agencyTokenCapacityService.hasSpaceAvailable(agencyToken));
    }

    @Test
    public void shouldReturnFalseIfNoSpaceAvailableWhereCapacityReduced() {
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);
        agencyToken.setCapacity(100L);

        when(identityRepository.countByAgencyTokenUid(UID)).thenReturn(101L);

        assertFalse(agencyTokenCapacityService.hasSpaceAvailable(agencyToken));
    }

    @Test
    public void shouldReturnSpacesUsedByAgencyToken() {
        AgencyToken expected = new AgencyToken(100L);

        when(identityRepository.countByAgencyTokenUid(UID)).thenReturn(100L);

        assertEquals(expected, agencyTokenCapacityService.getSpacesUsedByAgencyToken(UID));
    }

    @Test
    public void shouldReturnCountByAgencyToken() {
        when(identityRepository.countByAgencyTokenUid(UID)).thenReturn(100L);

        Long countOfAgencyByUid = agencyTokenCapacityService.getCountOfAgencyByUid(UID);

        assertEquals(Long.valueOf(100), countOfAgencyByUid);
    }

    @Test
    public void deleteAgencyToken_shouldCallRemoveAgencyToken() {
        String agencyToken = UUID.randomUUID().toString();
        agencyTokenCapacityService.deleteAgencyToken(agencyToken);
        verify(identityRepository, times(1)).removeAgencyToken(agencyToken);
    }
}
