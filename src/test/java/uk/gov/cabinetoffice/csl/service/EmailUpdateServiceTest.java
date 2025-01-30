package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.repository.EmailUpdateRepository;

import java.util.*;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus.*;

@SpringBootTest
@ActiveProfiles("no-redis")
@Transactional
public class EmailUpdateServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String NEW_EMAIL_ADDRESS = "new@newexample.com";
    private static final String CODE = "abc123";
    private static final String UID = "uid123";
    private static final String PASSWORD = "password";
    private static final Set<Role> ROLES = new HashSet<>();
    private static final Identity IDENTITY = new Identity(UID, EMAIL, PASSWORD, true, false, ROLES,
            now(), false, 0);

    @MockBean
    private EmailUpdateRepository emailUpdateRepository;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private CsrsService csrsService;

    @MockBean
    private NotifyService notifyService;

    @Captor
    private ArgumentCaptor<Identity> identityArgumentCaptor;

    @Autowired
    private EmailUpdateService emailUpdateService;

    @Test
    public void givenAValidPendingEmailUpdate_thenIsEmailUpdateExpiredShouldReturnFalse() {
        EmailUpdate emailUpdate = createPendingEmailUpdate();
        assertFalse(emailUpdateService.isEmailUpdateExpired(emailUpdate));
    }

    @Test
    public void givenAExpiredEmailUpdate_thenIsEmailUpdateExpiredShouldReturnTrue() {
        EmailUpdate emailUpdate = createPendingEmailUpdate();
        emailUpdate.setEmailUpdateStatus(EXPIRED);
        assertTrue(emailUpdateService.isEmailUpdateExpired(emailUpdate));
    }

    @Test
    public void givenAUpdatedEmailUpdate_thenIsEmailUpdateExpiredShouldReturnTrue() {
        EmailUpdate emailUpdate = createPendingEmailUpdate();
        emailUpdate.setEmailUpdateStatus(UPDATED);
        assertTrue(emailUpdateService.isEmailUpdateExpired(emailUpdate));
    }

    @Test
    public void givenAPendingEmailUpdateOlderThanValidityDuration_thenIsEmailUpdateExpiredShouldReturnTrue() {
        EmailUpdate emailUpdate = createPendingEmailUpdate();
        emailUpdate.setRequestedAt(emailUpdate.getRequestedAt().minusSeconds(86400));
        assertTrue(emailUpdateService.isEmailUpdateExpired(emailUpdate));
    }

    @Test
    public void giveMultiplePendingEmailUpdateExist_thenSaveEmailUpdateAndNotifyShouldReturnTrue() {
        EmailUpdate emailUpdate1 = createPendingEmailUpdate();
        EmailUpdate emailUpdate2 = createPendingEmailUpdate();
        List<EmailUpdate> pendingEmailUpdates = new ArrayList<>();
        pendingEmailUpdates.add(emailUpdate1);
        pendingEmailUpdates.add(emailUpdate2);
        when(emailUpdateRepository.findByNewEmailIgnoreCaseAndPreviousEmailIgnoreCaseAndEmailUpdateStatus(
                NEW_EMAIL_ADDRESS, IDENTITY.getEmail(), PENDING)).thenReturn(pendingEmailUpdates);
        when(emailUpdateRepository.saveAll(pendingEmailUpdates)).thenReturn(pendingEmailUpdates);
        EmailUpdate emailUpdate3 = createPendingEmailUpdate();
        when(emailUpdateRepository.save(any())).thenReturn(emailUpdate3);
        doNothing().when(notifyService).notifyWithPersonalisation(eq(NEW_EMAIL_ADDRESS), any(), any());

        assertTrue(emailUpdateService.saveEmailUpdateAndNotify(IDENTITY, NEW_EMAIL_ADDRESS));
    }

    @Test
    public void givenAValidCodeForIdentity_whenVerifyCode_thenReturnsTrue() {
        when(emailUpdateRepository.existsByCode(anyString())).thenReturn(true);
        boolean actual = emailUpdateService.isEmailUpdateRequestExistsForCode("co");
        assertTrue(actual);
        verify(emailUpdateRepository, times(1)).existsByCode(eq("co"));
    }

    @Test
    public void givenAInvalidCodeForIdentity_whenVerifyCode_thenReturnsFalse() {
        when(emailUpdateRepository.existsByCode(anyString())).thenReturn(false);
        boolean actual = emailUpdateService.isEmailUpdateRequestExistsForCode("co");
        assertFalse(actual);
        verify(emailUpdateRepository, times(1)).existsByCode(eq("co"));
    }

    @Test
    public void givenAValidIdentity_whenNewDomainAllowListedAndNotAgency_shouldReturnSuccessfully() {
        EmailUpdate emailUpdate = createPendingEmailUpdate();

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(IDENTITY);
        doNothing().when(identityService).updateEmailAddress(eq(IDENTITY), eq(emailUpdate.getNewEmail()), isNull());

        emailUpdateService.updateEmailAddress(emailUpdate);

        verify(csrsService, times(1)).removeOrganisationalUnitFromCivilServant(any());
        verify(identityService, times(1)).updateEmailAddress(identityArgumentCaptor.capture(),
                eq(emailUpdate.getNewEmail()), isNull());

        Identity identityArgumentCaptorValue = identityArgumentCaptor.getValue();
        assertThat(identityArgumentCaptorValue.getEmail(), equalTo(EMAIL));
    }

    @Test
    public void givenAValidIdentity_whenNewDomainIsAgency_shouldReturnSuccessfully() {

        EmailUpdate emailUpdate = createPendingEmailUpdate();

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(IDENTITY);
        doNothing().when(identityService).updateEmailAddress(eq(IDENTITY), eq(emailUpdate.getNewEmail()), eq(agencyToken));

        emailUpdateService.updateEmailAddress(emailUpdate, agencyToken);

        verify(csrsService, times(1)).removeOrganisationalUnitFromCivilServant(any());
        verify(identityService, times(1)).updateEmailAddress(identityArgumentCaptor.capture(),
                eq(emailUpdate.getNewEmail()), eq(agencyToken));

        Identity identityArgumentCaptorValue = identityArgumentCaptor.getValue();
        assertThat(identityArgumentCaptorValue.getEmail(), equalTo(EMAIL));
    }

    @Test
    public void shouldGetEmailUpdate() {
        EmailUpdate emailUpdate = createPendingEmailUpdate();
        when(emailUpdateRepository.findByCode(anyString())).thenReturn(Optional.of(emailUpdate));
        assertEquals(emailUpdateService.getEmailUpdateRequestForCode(CODE), emailUpdate);
    }

    private EmailUpdate createPendingEmailUpdate() {
        EmailUpdate emailUpdate = new EmailUpdate();
        emailUpdate.setId(100L);
        emailUpdate.setCode(CODE);
        emailUpdate.setPreviousEmail(EMAIL);
        emailUpdate.setNewEmail(NEW_EMAIL_ADDRESS);
        emailUpdate.setIdentity(IDENTITY);
        emailUpdate.setEmailUpdateStatus(PENDING);
        emailUpdate.setRequestedAt(now());
        return emailUpdate;
    }
}
