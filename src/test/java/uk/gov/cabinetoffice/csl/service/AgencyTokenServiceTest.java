package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.service.client.csrs.CivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("no-redis")
public class AgencyTokenServiceTest {
    private static final String DOMAIN = "someone@kainos.com";

    @Mock
    private IdentityService identityService;

    @Mock
    private CivilServantRegistryClient civilServantRegistryClient;

    @InjectMocks
    private AgencyTokenService agencyTokenService;

    @Test
    public void givenAllowListedDomain_whenIsDomainAllowListed_thenShouldReturnTrue() {
        when(identityService.isAllowListedDomain(anyString())).thenReturn(true);

        boolean actual = agencyTokenService.isDomainAllowListed(DOMAIN);

        assertTrue(actual);
    }

    @Test
    public void givenANonAllowListedDomain_whenIsDomainAllowListed_thenShouldReturnFalse() {
        when(identityService.isAllowListedDomain(anyString())).thenReturn(false);

        boolean actual = agencyTokenService.isDomainAllowListed(DOMAIN);

        assertFalse(actual);
    }

    @Test
    public void givenNonAllowListedDomainWithAgencyTokenDomains_whenIsDomainAnAgencyTokenDomain_thenShouldReturnTrue() {
        AgencyToken[] agencyTokens = new AgencyToken[3];
        agencyTokens[0] = new AgencyToken();
        agencyTokens[1] = new AgencyToken();
        agencyTokens[2] = new AgencyToken();
        when(civilServantRegistryClient.getAgencyTokensForDomain(anyString())).thenReturn(agencyTokens);

        boolean actual = agencyTokenService.isDomainAnAgencyTokenDomain(DOMAIN);

        assertTrue(actual);
    }

    @Test
    public void givenNonAllowListedDomainWithNoAgencyTokenDomains_whenIsDomainAnAgencyTokenDomain_thenShouldReturnFalse() {
        AgencyToken [] agencyTokens = new AgencyToken[0];
        when(civilServantRegistryClient.getAgencyTokensForDomain(anyString())).thenReturn(agencyTokens);

        boolean actual = agencyTokenService.isDomainAnAgencyTokenDomain(DOMAIN);

        assertFalse(actual);
    }

    @Test
    public void shouldReturnTrueIfDomainInAgency() {
        when(civilServantRegistryClient.isDomainInAgency(DOMAIN)).thenReturn(true);

        assertTrue(agencyTokenService.isDomainInAgencyToken(DOMAIN));
    }

    @Test
    public void shouldReturnFalseIfDomainInAgency() {
        when(civilServantRegistryClient.isDomainInAgency(DOMAIN)).thenReturn(false);

        assertFalse(agencyTokenService.isDomainInAgencyToken(DOMAIN));
    }
}
