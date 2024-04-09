package uk.gov.cabinetoffice.csl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.InviteStatus;

import java.time.Clock;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class InviteRepositoryTest {

    public static final String INVITE_CODE = "abc123";
    public static final String INVITE_FOR_EMAIL = "test@example.org";

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private Clock clock;

    @Test
    public void findByForEmailShouldReturnCorrectInvite() {
        Invite invite = createInvite();
        inviteRepository.save(invite);
        Invite actualInvite = inviteRepository.findByForEmailIgnoreCase(INVITE_FOR_EMAIL);
        assertThat(actualInvite.getCode(), equalTo(INVITE_CODE));
        assertThat(actualInvite.getForEmail(), equalTo(INVITE_FOR_EMAIL));
    }

    @Test
    public void findByCodeShouldReturnCorrectInvite() {
        Invite invite = createInvite();
        inviteRepository.save(invite);
        Invite actualInvite = inviteRepository.findByCode(INVITE_CODE);
        assertThat(actualInvite.getCode(), equalTo(INVITE_CODE));
        assertThat(actualInvite.getForEmail(), equalTo(INVITE_FOR_EMAIL));
    }

    @Test
    public void inviteShouldNotExistByCodeIfNotPresent() {
        assertThat(inviteRepository.existsByCode(INVITE_CODE), equalTo(false));
    }

    @Test
    public void inviteShouldExistByCode() {
        Invite invite = createInvite();
        inviteRepository.save(invite);
        assertThat(inviteRepository.existsByCode(INVITE_CODE), equalTo(true));
    }

    @Test
    public void existsByCodeAndStatusReturnsCorrectResult() {
        final String pendingEmail = "pending@example.org";
        final String expiredEmail = "expired@example.org";
        inviteRepository.save(createInvite("code", pendingEmail));
        Invite expiredInvite = createInvite("code2", expiredEmail);
        expiredInvite.setStatus(InviteStatus.EXPIRED);
        inviteRepository.save(expiredInvite);
        boolean existsByCodeAndStatusForPendingInvite = inviteRepository.existsByForEmailIgnoreCaseAndStatus(pendingEmail, InviteStatus.PENDING);
        boolean existsByCodeAndStatusForExpiredInvite = inviteRepository.existsByForEmailIgnoreCaseAndStatus(expiredEmail, InviteStatus.PENDING);
        assertThat(existsByCodeAndStatusForPendingInvite, equalTo(true));
        assertThat(existsByCodeAndStatusForExpiredInvite, equalTo(false));
    }

    @Test
    public void findByForEmailAndStatusShouldReturnCorrectInvite() {
        Invite invite = createInvite();
        inviteRepository.save(invite);
        Optional<Invite> actualInvite = inviteRepository.findByForEmailIgnoreCaseAndStatus(INVITE_FOR_EMAIL, InviteStatus.PENDING);
        assertThat(actualInvite.get().getCode(), equalTo(INVITE_CODE));
        assertThat(actualInvite.get().getForEmail(), equalTo(INVITE_FOR_EMAIL));
        assertThat(actualInvite.get().getStatus(), equalTo(InviteStatus.PENDING));
    }

    private Invite createInvite() {
        return createInvite(INVITE_CODE, INVITE_FOR_EMAIL);
    }

    private Invite createInvite(String code, String forEmail) {
        Identity identity = identityRepository.findFirstByActiveTrueAndEmailEqualsIgnoreCase("learner@domain.com");
        Invite invite = new Invite();
        invite.setInviter(identity);
        invite.setCode(code);
        invite.setForEmail(forEmail);
        invite.setInvitedAt(now(clock));
        invite.setStatus(InviteStatus.PENDING);
        return invite;
    }
}
