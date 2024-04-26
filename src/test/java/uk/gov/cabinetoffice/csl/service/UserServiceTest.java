package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.LocalDateTime;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class UserServiceTest {

    private UserService userService;

    @Mock
    private IdentityService identityService;

    @Mock
    private ReactivationService reactivationService;

    private final Utils utils = new Utils();

    @BeforeEach
    public void setUp() {
        userService = new UserService(identityService, reactivationService, utils);
    }

    @Test
    public void shouldLoadIdentityByEmailAddress() {

        String email = "test@example.org";
        String domain = "example.org";
        String uid = "uid";
        String agencyTokenUid = "agencyTokenUid";
        Identity identity = new Identity(uid, email, "password", true, false,
                emptySet(), LocalDateTime.now(), false, agencyTokenUid, null);

        when(identityService.getIdentityForEmail(email)).thenReturn(identity);
        when(identityService.isDomainAllowListed(domain)).thenReturn(true);
        when(identityService.isDomainInAnAgencyToken(domain)).thenReturn(true);
        when(identityService.isEmailInvited(email)).thenReturn(true);

        IdentityDetails identityDetails = (IdentityDetails) userService.loadUserByUsername(email);

        assertThat(identityDetails, notNullValue());
        assertThat(identityDetails.getUsername(), equalTo(uid));
        assertThat(identityDetails.getIdentity(), equalTo(identity));
    }

    @Test
    public void shouldThrowErrorWhenNoClientFound() {

        String email = "test@example.org";

        when(identityService.getIdentityForEmail(email)).thenReturn(null);

        UsernameNotFoundException thrown = assertThrows(
                UsernameNotFoundException.class, () -> userService.loadUserByUsername(email));

        assertEquals("No user found with email address " + email, thrown.getMessage());
    }
}
