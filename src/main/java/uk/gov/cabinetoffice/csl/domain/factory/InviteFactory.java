package uk.gov.cabinetoffice.csl.domain.factory;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.InviteStatus;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.repository.RoleRepository;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class InviteFactory {
    private static final String LEARNER_ROLE_NAME = "LEARNER";
    private final RoleRepository roleRepository;

    public InviteFactory(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Invite create(String email, Set<Role> roleSet, Identity inviter) {
        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setForRoles(roleSet);
        invite.setInviter(inviter);
        invite.setInvitedAt(new Date());
        invite.setStatus(InviteStatus.PENDING);
        invite.setCode(RandomStringUtils.random(40, true, true));

        return invite;
    }

    public Invite createSelfSignUpInvite(String email) {
        Role role = roleRepository.findFirstByNameEquals(LEARNER_ROLE_NAME);

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setForRoles(new HashSet<>(Collections.singletonList(role)));
        invite.setInvitedAt(new Date());
        invite.setStatus(InviteStatus.PENDING);
        invite.setCode(RandomStringUtils.random(40, true, true));

        return invite;
    }
}
