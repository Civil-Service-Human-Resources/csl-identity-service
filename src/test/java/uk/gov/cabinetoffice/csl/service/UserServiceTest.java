package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.cabinetoffice.csl.client.csrs.CivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String UID = "uid123";
    private final String[] allowListedDomains = new String[]{"allowlisted.gov.uk", "example.com"};

    private UserService userService;

    @Mock(name="identityRepository")
    private IdentityRepository identityRepository;

    @Mock
    private InviteService inviteService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CivilServantRegistryClient civilServantRegistryClient;

    @Mock
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @BeforeEach
    public void setUp() {
        userService = new UserService(
                inviteService,
                civilServantRegistryClient,
                agencyTokenCapacityService,
                identityRepository,
                passwordEncoder,
                allowListedDomains
        );
    }

    @Test
    public void shouldLoadIdentityByEmailAddress() {

        final String emailAddress = "test@example.org";
        final String uid = "uid";
        final Identity identity = new Identity(uid, emailAddress, "password", true, false,
                emptySet(), Instant.now(), false, null);

        when(identityRepository.findFirstByEmailEquals(emailAddress))
                .thenReturn(identity);

        IdentityDetails identityDetails = (IdentityDetails) userService.loadUserByUsername(emailAddress);

        assertThat(identityDetails, notNullValue());
        assertThat(identityDetails.getUsername(), equalTo(uid));
        assertThat(identityDetails.getIdentity(), equalTo(identity));
    }

    @Test
    public void shouldThrowErrorWhenNoClientFound() {

        final String emailAddress = "test@example.org";

        when(identityRepository.findFirstByEmailEquals(emailAddress))
                .thenReturn(null);

        UsernameNotFoundException thrown = assertThrows(
                UsernameNotFoundException.class, () -> userService.loadUserByUsername(emailAddress));

        assertEquals("No user found with email address: " + emailAddress, thrown.getMessage());
    }

    @Test
    public void shouldReturnTrueWhenInvitingAnExistingUser() {
        final String emailAddress = "test@example.org";

        when(identityRepository.existsByEmail(emailAddress))
                .thenReturn(true);

        assertThat(userService.existsByEmail("test@example.org"), equalTo(true));
    }

    @Test
    public void shouldReturnFalseWhenInvitingAnNonExistingUser() {
        assertThat(userService.existsByEmail("test2@example.org"), equalTo(false));
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

        TokenRequest tokenRequest = new TokenRequest();

        when(inviteService.findByCode(code)).thenReturn(invite);

        when(passwordEncoder.encode("password")).thenReturn("password");

        userService.createIdentityFromInviteCode(code, "password", tokenRequest);

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

        TokenRequest tokenRequest = new TokenRequest();
        String tokenDomain = "example.com";
        String tokenCode = "co";
        String tokenToken = "token123";
        tokenRequest.setDomain(tokenDomain);
        tokenRequest.setOrg(tokenCode);
        tokenRequest.setToken(tokenToken);

        String uid = "UID";
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(uid);

        when(inviteService.findByCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(tokenDomain, tokenToken, tokenCode)).thenReturn(Optional.of(agencyToken));
        when(passwordEncoder.encode("password")).thenReturn("password");
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(true);

        userService.createIdentityFromInviteCode(code, "password", tokenRequest);

        ArgumentCaptor<Identity> inviteArgumentCaptor = ArgumentCaptor.forClass(Identity.class);

        verify(identityRepository).save(inviteArgumentCaptor.capture());

        Identity identity = inviteArgumentCaptor.getValue();
        assertThat(identity.getRoles().contains(role), equalTo(true));
        assertThat(identity.getPassword(), equalTo("password"));
        assertThat(identity.getEmail(), equalTo("test@example.com"));
        assertThat(identity.getAgencyTokenUid(), equalTo(uid));
    }

    @Test
    public void lockIdentitySetsLockedToTrue() {
        String email = "test-email";
        Identity identity = mock(Identity.class);
        when(identityRepository.findFirstByActiveTrueAndEmailEquals(email)).thenReturn(identity);

        userService.lockIdentity(email);

        InOrder inOrder = inOrder(identity, identityRepository);

        inOrder.verify(identity).setLocked(true);
        inOrder.verify(identityRepository).save(identity);
    }

    @Test
    public void shouldGetIdentityByEmailAndActiveFalse() {
        Identity identity = mock(Identity.class);
        when(identity.getUid()).thenReturn(UID);
        when(identity.isActive()).thenReturn(false);

        when(identityRepository.findFirstByActiveFalseAndEmailEquals(EMAIL)).thenReturn(Optional.of(identity));

        Identity actualIdentity = userService.getIdentityByEmailAndActiveFalse(EMAIL);

        assertEquals(UID, actualIdentity.getUid());
        assertFalse(actualIdentity.isActive());
    }

    @Test
    public void shouldThrowExceptionIfIdentityNotFound() {
        doThrow(new IdentityNotFoundException("Identity not found")).when(identityRepository).findFirstByActiveFalseAndEmailEquals(EMAIL);

        IdentityNotFoundException thrown = assertThrows(
                IdentityNotFoundException.class, () -> userService.getIdentityByEmailAndActiveFalse(EMAIL));

        assertEquals("Identity not found", thrown.getMessage());
    }

    @Test
    public void testIsAllowListedDomainMixedCase(){
        boolean validDomain = userService.isAllowListedDomain("ExAmPlE.cOm");

        assertTrue(validDomain);
    }

    @Test
    public void testIsAllowListedDomainLowerCase(){
        boolean validDomain = userService.isAllowListedDomain("example.com");

        assertTrue(validDomain);
    }

    @Test
    public void testIsAllowListedDomainUpperCase(){
        boolean validDomain = userService.isAllowListedDomain("EXAMPLE.COM");

        assertTrue(validDomain);
    }
}
