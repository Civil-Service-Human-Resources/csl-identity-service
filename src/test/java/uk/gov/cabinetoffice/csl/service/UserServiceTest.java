package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

import java.time.Instant;

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

    @Mock(name="identityRepository")
    private IdentityRepository identityRepository;

    @Mock(name="reactivationService")
    private ReactivationService reactivationService;

    @BeforeEach
    public void setUp() {
        userService = new UserService(identityRepository, reactivationService);
    }

    @Test
    public void shouldLoadIdentityByEmailAddress() {

        final String emailAddress = "test@example.org";
        final String uid = "uid";
        final Identity identity = new Identity(uid, emailAddress, "password", true, false,
                emptySet(), Instant.now(), false, null);

        when(identityRepository.findFirstByEmailEqualsIgnoreCase(emailAddress))
                .thenReturn(identity);

        IdentityDetails identityDetails = (IdentityDetails) userService.loadUserByUsername(emailAddress);

        assertThat(identityDetails, notNullValue());
        assertThat(identityDetails.getUsername(), equalTo(uid));
        assertThat(identityDetails.getIdentity(), equalTo(identity));
    }

    @Test
    public void shouldThrowErrorWhenNoClientFound() {

        final String emailAddress = "test@example.org";

        when(identityRepository.findFirstByEmailEqualsIgnoreCase(emailAddress))
                .thenReturn(null);

        UsernameNotFoundException thrown = assertThrows(
                UsernameNotFoundException.class, () -> userService.loadUserByUsername(emailAddress));

        assertEquals("No user found with email address " + emailAddress, thrown.getMessage());
    }
}
