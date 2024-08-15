package uk.gov.cabinetoffice.csl.factory;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.InviteStatus;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.repository.RoleRepository;

import java.time.Clock;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.time.LocalDateTime.now;

@Component
public class InviteFactory {

    private static final String LEARNER_ROLE_NAME = "LEARNER";
    private final RoleRepository roleRepository;
    private final Clock clock;

    public InviteFactory(RoleRepository roleRepository, Clock clock) {
        this.roleRepository = roleRepository;
        this.clock = clock;
    }

    public Invite create(String email, Set<Role> roleSet, Identity inviter) {
        Invite invite = createInvite(email, roleSet);
        invite.setInviter(inviter);
        return invite;
    }

    public Invite createSelfSignUpInvite(String email) {
        Role role = roleRepository.findFirstByNameEquals(LEARNER_ROLE_NAME);
        return createInvite(email, new HashSet<>(Collections.singletonList(role)));
    }

    private Invite createInvite(String email, Set<Role> roleSet) {
        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setForRoles(roleSet);
        invite.setInvitedAt(now(clock));
        invite.setStatus(InviteStatus.PENDING);
        invite.setCode(RandomStringUtils.random(40, true, true));
        return invite;
    }
}
