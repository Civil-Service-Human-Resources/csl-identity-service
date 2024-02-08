package uk.gov.cabinetoffice.csl.util;

import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class TestUtil {

    public static IdentityDetails createIdentityDetails(Long id, String uid, String email, String password) {
        return new IdentityDetails(createIdentity(id, uid, email, password, "agencyTokenUid"));
    }

    public static Identity createIdentity(Long id, String uid, String email, String password, String agencyTokenUid) {
        Identity identity = createIdentity(uid, email, password, agencyTokenUid);
        identity.setId(id);
        return identity;
    }

    public static Identity createIdentity(String uid, String email, String password, String agencyTokenUid) {
        return new Identity(uid, email, password, true, false, createRoles(),
                Instant.now(), false, agencyTokenUid, 0);
    }

    public static Set<Role> createRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(new Role("LEARNER", "Learner"));
        roles.add(new Role("LEARNING_MANAGER", "Learning Manager"));
        roles.add(new Role("IDENTITY_MANAGER", "Identity Manager"));
        roles.add(new Role("CSHR_REPORTER", "CSHR Reporter"));
        roles.add(new Role("DOWNLOAD_BOOKING_FEED", "Download Booking Feed"));
        roles.add(new Role("ORGANISATION_MANAGER", "Organisation Manager"));
        roles.add(new Role("PROFESSION_MANAGER", "Profession Manager"));
        return roles;
    }
}
