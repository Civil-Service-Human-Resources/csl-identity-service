package uk.gov.cabinetoffice.csl.util;

import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

public class TestUtil {

    public static final String EMAIL_TEMPLATE = "%s@example.org";
    public static final String PASSWORD = "password123";

    public static Identity createIdentity() {
        return createIdentity(null);
    }

    public static Identity createIdentity(String agencyTokenUid) {
        return createIdentity(randomUUID().toString(), randomUUID().toString(), agencyTokenUid);
    }

    public static Identity createIdentity(String uid, String emailPrefix, String agencyTokenUid) {
        return new Identity(uid, format(EMAIL_TEMPLATE, emailPrefix), PASSWORD, true, false,
                null, Instant.now(), false, agencyTokenUid, 0);
    }

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
        roles.add(new Role(1L, "LEARNER", "Learner"));
        roles.add(new Role(2L, "LEARNING_MANAGER", "Learning Manager"));
        roles.add(new Role(3L, "IDENTITY_MANAGER", "Identity Manager"));
        roles.add(new Role(4L, "CSHR_REPORTER", "CSHR Reporter"));
        roles.add(new Role(5L, "DOWNLOAD_BOOKING_FEED", "Download Booking Feed"));
        roles.add(new Role(6L, "ORGANISATION_MANAGER", "Organisation Manager"));
        roles.add(new Role(7L, "PROFESSION_MANAGER", "Profession Manager"));
        return roles;
    }
}
