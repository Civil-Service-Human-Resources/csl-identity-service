package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.BatchProcessResponse;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.repository.CompoundRolesRepository;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.util.TestUtil.createIdentity;

@SpringBootTest
@ActiveProfiles("no-redis")
public class IdentityServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String UID = "uid123";

    private IdentityService identityService;

    @Mock
    private InviteService inviteService;

    @Mock
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private ICivilServantRegistryClient civilServantRegistryClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final Utils utils = new Utils();

    private final Clock clock = Clock.fixed(Instant.parse("2024-03-01T00:00:00Z"), ZoneId.of("Europe/London"));

    @BeforeEach
    public void setUp() {
        identityService = new IdentityService(
                inviteService,
                agencyTokenCapacityService,
                identityRepository,
                new CompoundRolesRepository(),
                civilServantRegistryClient,
                passwordEncoder,
                utils,
                clock
        );
        when(civilServantRegistryClient.getAllowListDomains()).thenReturn(asList("allowListed.gov.uk", "example.com"));
    }

    @Test
    public void shouldLoadIdentityByEmailAddress() {

        final String emailAddress = "test@example.org";
        final String uid = "uid";
        final Identity identity = new Identity(uid, emailAddress, "password", true, false,
                emptySet(), LocalDateTime.now(), false, null);

        when(identityRepository.findFirstByEmailEqualsIgnoreCase(emailAddress))
                .thenReturn(identity);

        Identity actualIdentity =  identityService.getIdentityForEmail(emailAddress);

        assertThat(actualIdentity, notNullValue());
        assertThat(actualIdentity.getUid(), equalTo(uid));
        assertThat(actualIdentity, equalTo(identity));
    }

    @Test
    public void shouldReturnTrueWhenInvitingAnExistingUser() {
        final String emailAddress = "test@example.org";

        when(identityRepository.existsByEmailIgnoreCase(emailAddress)).thenReturn(true);

        assertThat(identityService.isIdentityExistsForEmail("test@example.org"), equalTo(true));
    }

    @Test
    public void shouldReturnFalseWhenInvitingAnNonExistingUser() {
        assertThat(identityService.isIdentityExistsForEmail("test2@example.org"), equalTo(false));
    }

    @Test
    public void createIdentityFromInviteCodeWithoutAgencyButIsAllowListed() {
        final String code = "123abc";
        final String email = "test@example.com";
        Role role = new Role();
        role.setName("USER");

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        Invite invite = new Invite();
        invite.setCode(code);
        invite.setForEmail(email);
        invite.setForRoles(roles);

        AgencyToken agencyToken = new AgencyToken();

        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(passwordEncoder.encode("password")).thenReturn("password");

        identityService.createIdentityFromInviteCode(code, "password", agencyToken);

        ArgumentCaptor<Identity> inviteArgumentCaptor = ArgumentCaptor.forClass(Identity.class);

        verify(identityRepository).save(inviteArgumentCaptor.capture());

        Identity identity = inviteArgumentCaptor.getValue();
        assertThat(identity.getRoles().contains(role), equalTo(true));
        assertThat(identity.getPassword(), equalTo("password"));
        assertThat(identity.getEmail(), equalTo("test@example.com"));
        assertThat(identity.getAgencyTokenUid(), equalTo(null));
    }

    @Test
    public void createIdentityFromInviteCodeWithAgency() {
        final String code = "123abc";
        final String email = "test@example.com";
        Role role = new Role();
        role.setName("USER");

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        Invite invite = new Invite();
        invite.setCode(code);
        invite.setForEmail(email);
        invite.setForRoles(roles);

        String uid = "UID";
        String tokenDomain = "example.com";
        String tokenCode = "co";
        String tokenToken = "token123";

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(uid);
        agencyToken.setDomain(tokenDomain);
        agencyToken.setOrg(tokenCode);
        agencyToken.setToken(tokenToken);

        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(tokenDomain, tokenToken, tokenCode))
                .thenReturn(Optional.of(agencyToken));
        when(passwordEncoder.encode("password")).thenReturn("password");
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(true);

        identityService.createIdentityFromInviteCode(code, "password", agencyToken);

        ArgumentCaptor<Identity> inviteArgumentCaptor = ArgumentCaptor.forClass(Identity.class);

        verify(identityRepository).save(inviteArgumentCaptor.capture());

        Identity identity = inviteArgumentCaptor.getValue();
        assertThat(identity.getRoles().contains(role), equalTo(true));
        assertThat(identity.getPassword(), equalTo("password"));
        assertThat(identity.getEmail(), equalTo("test@example.com"));
        assertThat(identity.getAgencyTokenUid(), equalTo(uid));
    }

    @Test
    public void shouldGetIdentityByEmailAndActiveFalse() {
        Identity identity = mock(Identity.class);
        when(identity.getUid()).thenReturn(UID);
        when(identity.isActive()).thenReturn(false);

        when(identityRepository.findFirstByActiveFalseAndEmailEqualsIgnoreCase(EMAIL)).thenReturn(Optional.of(identity));

        Identity actualIdentity = identityService.getInactiveIdentityForEmail(EMAIL);

        assertEquals(UID, actualIdentity.getUid());
        assertFalse(actualIdentity.isActive());
    }

    @Test
    public void shouldThrowExceptionIfIdentityNotFound() {
        doThrow(new IdentityNotFoundException("Identity not found")).when(identityRepository).findFirstByActiveFalseAndEmailEqualsIgnoreCase(EMAIL);

        IdentityNotFoundException thrown = assertThrows(
                IdentityNotFoundException.class, () -> identityService.getInactiveIdentityForEmail(EMAIL));

        assertEquals("Identity not found", thrown.getMessage());
    }

    @Test
    public void testIsAllowListedDomainMixedCase(){
        boolean validDomain = identityService.isAllowListedDomain("ExAmPlE.cOm");
        assertTrue(validDomain);
    }

    @Test
    public void testIsAllowListedDomainLowerCase(){
        boolean validDomain = identityService.isAllowListedDomain("example.com");
        assertTrue(validDomain);
    }

    @Test
    public void testIsAllowListedDomainUpperCase(){
        boolean validDomain = identityService.isAllowListedDomain("EXAMPLE.COM");
        assertTrue(validDomain);
    }

    @Test
    public void shouldRemoveReportingRoles() {
        Role orgReporter = new Role("ORGANISATION_REPORTER", "");
        Role professionReporter = new Role("PROFESSION_REPORTER", "");
        Role learner = new Role("LEARNER", "");

        Identity reporter = createIdentity("uid123", "reporter@email.com", null);
        reporter.setRoles(new HashSet<>(asList(learner, orgReporter)));

        Identity reporter1 = createIdentity("uid456", "reporter1@email.com", null);
        reporter1.setRoles(new HashSet<>(asList(learner, orgReporter, professionReporter)));

        Identity user = createIdentity("uid789", "user@email.com", null);
        user.setRoles(new HashSet<>(Collections.singletonList(learner)));
        List<Identity> identities = asList(reporter, reporter1, user);

        when(identityRepository.findIdentitiesByUids(asList("uid123", "uid456", "uid789"))).thenReturn(identities);
        BatchProcessResponse resp = identityService.removeReportingRoles(asList("uid123", "uid456", "uid789"));
        List<String> successfulIds = resp.getSuccessfulIds();
        assertEquals(2, successfulIds.size());
        assertEquals("uid123", successfulIds.get(0));
        assertEquals("uid456", successfulIds.get(1));
    }
}
