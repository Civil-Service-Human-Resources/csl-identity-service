package uk.gov.cabinetoffice.csl.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.repository.RoleRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InviteFactoryTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private InviteFactory inviteFactory;

    @Test
    public void shouldReturnInvite() {
        String email = "user@domain.org";
        Identity inviter = new Identity();
        Role role1 = new Role(1L, "role1", "description");
        Role role2 = new Role(2L, "role2", "description");
        Set<Role> roleSet = new HashSet<>(Arrays.asList(role1, role2));
        Invite invite = inviteFactory.create(email, roleSet, inviter);
        assertEquals(email, invite.getForEmail());
        assertEquals(roleSet, invite.getForRoles());
        assertEquals(inviter, invite.getInviter());
        assertNotNull(invite.getInvitedAt());
        assertNotNull(invite.getCode());
        assertEquals(40, invite.getCode().length());
    }

    @Test
    public void shouldReturnSelfSignUpInvite() {
        String email = "user@domain.org";
        Role role = new Role(1L, "role1", "description");
        when(roleRepository.findFirstByNameEquals("LEARNER")).thenReturn(role);
        Invite invite = inviteFactory.createSelfSignUpInvite(email);
        assertEquals(email, invite.getForEmail());
        assertEquals(new HashSet<>(Collections.singletonList(role)), invite.getForRoles());
        assertNull(invite.getInviter());
        assertEquals(new Date().toString(), invite.getInvitedAt().toString());
        assertNotNull(invite.getCode());
        assertEquals(40, invite.getCode().length());
    }
}
