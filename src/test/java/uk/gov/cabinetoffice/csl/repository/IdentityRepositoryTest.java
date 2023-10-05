package uk.gov.cabinetoffice.csl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class IdentityRepositoryTest {

    public static final String EMAIL_TEMPLATE = "%s@example.org";
    public static final String PASSWORD = "password123";

    @Autowired
    private IdentityRepository identityRepository;

    @Test
    public void findByForEmailShouldReturnCorrectInvite() {
        Identity identity = createIdentity();
        identityRepository.save(identity);

        assertThat(identityRepository.existsByEmail(identity.getEmail()), equalTo(true));
        assertThat(identityRepository.existsByEmail("doesntexist@example.com"), equalTo(false));

    }

    @Test
    public void removeAgencyToken_shouldRemoveAgencyTokenAndSetInactiveOnSingleMatch() {

        Identity originalIdentity = createIdentity(UUID.randomUUID().toString());
        Identity otherAgencyTokenIdentity = createIdentity(UUID.randomUUID().toString());
        Identity otherNonAgencyIdentity = createIdentity();

        identityRepository.saveAndFlush(originalIdentity);
        identityRepository.saveAndFlush(otherAgencyTokenIdentity);
        identityRepository.saveAndFlush(otherNonAgencyIdentity);

        identityRepository.removeAgencyToken(originalIdentity.getAgencyTokenUid());

        Identity updatedIdentity = identityRepository.getOne(originalIdentity.getId());
        Identity postUpdateOtherAgencyTokenIdentity = identityRepository.getOne(otherAgencyTokenIdentity.getId());
        Identity postUpdateOtherNonAgencyIdentity = identityRepository.getOne(otherNonAgencyIdentity.getId());

        assertTrue(updatedIdentity.isActive());
        assertNull(updatedIdentity.getAgencyTokenUid());

        assertEquals(otherAgencyTokenIdentity.toString(), postUpdateOtherAgencyTokenIdentity.toString());
        assertEquals(otherNonAgencyIdentity.toString(), postUpdateOtherNonAgencyIdentity.toString());
    }

    @Test
    public void removeAgencyToken_shouldRemoveAgencyTokenAndSetInactiveOnMultiMatch() {

        String agencyTokenUid = UUID.randomUUID().toString();

        Identity originalIdentityOne = createIdentity(agencyTokenUid);
        Identity originalIdentityTwo = createIdentity(agencyTokenUid);
        Identity otherAgencyTokenIdentity = createIdentity(UUID.randomUUID().toString());
        Identity otherNonAgencyIdentity = createIdentity();

        identityRepository.saveAndFlush(originalIdentityOne);
        identityRepository.saveAndFlush(originalIdentityTwo);
        identityRepository.saveAndFlush(otherAgencyTokenIdentity);
        identityRepository.saveAndFlush(otherNonAgencyIdentity);

        identityRepository.removeAgencyToken(agencyTokenUid);

        Identity updatedIdentityOne = identityRepository.getOne(originalIdentityOne.getId());
        Identity updatedIdentityTwo = identityRepository.getOne(originalIdentityTwo.getId());
        Identity postUpdateOtherAgencyTokenIdentity = identityRepository.getOne(otherAgencyTokenIdentity.getId());
        Identity postUpdateOtherNonAgencyIdentity = identityRepository.getOne(otherNonAgencyIdentity.getId());

        assertTrue(updatedIdentityOne.isActive());
        assertNull(updatedIdentityOne.getAgencyTokenUid());

        assertTrue(updatedIdentityTwo.isActive());
        assertNull(updatedIdentityTwo.getAgencyTokenUid());

        assertEquals(otherAgencyTokenIdentity.toString(), postUpdateOtherAgencyTokenIdentity.toString());
        assertEquals(otherNonAgencyIdentity.toString(), postUpdateOtherNonAgencyIdentity.toString());
    }

    @Test
    public void removeAgencyToken_doesNotSetNonAgencyTokenIdentityToInactiveOnNullToken() {
        Identity originalIdentity = createIdentity(UUID.randomUUID().toString());
        Identity otherAgencyTokenIdentity = createIdentity(UUID.randomUUID().toString());
        Identity otherNonAgencyIdentity = createIdentity();

        identityRepository.saveAndFlush(originalIdentity);
        identityRepository.saveAndFlush(otherAgencyTokenIdentity);
        identityRepository.saveAndFlush(otherNonAgencyIdentity);

        identityRepository.removeAgencyToken(null);

        Identity updatedIdentity = identityRepository.getOne(originalIdentity.getId());
        Identity postUpdateOtherAgencyTokenIdentity = identityRepository.getOne(otherAgencyTokenIdentity.getId());
        Identity postUpdateOtherNonAgencyIdentity = identityRepository.getOne(otherNonAgencyIdentity.getId());

        assertEquals(originalIdentity.toString(), updatedIdentity.toString());
        assertEquals(otherAgencyTokenIdentity.toString(), postUpdateOtherAgencyTokenIdentity.toString());
        assertEquals(otherNonAgencyIdentity.toString(), postUpdateOtherNonAgencyIdentity.toString());
    }

    @Test
    public void findIdentitiesByUidsNormalised_shouldReturnIdentitiesForGivenUids() {

        String uid1 = UUID.randomUUID().toString();
        String uid2 = UUID.randomUUID().toString();
        String uid3 = UUID.randomUUID().toString();
        String uid4 = UUID.randomUUID().toString();

        Identity identity1 = createIdentity(uid1, uid1, null);
        Identity identity2 = createIdentity(uid2, uid2, "");
        Identity identity3 = createIdentity(uid3, uid3, "at");

        identityRepository.saveAndFlush(identity1);
        identityRepository.saveAndFlush(identity2);
        identityRepository.saveAndFlush(identity3);

        List<String> uids1 = new ArrayList<>();
        uids1.add(uid1);
        uids1.add(uid2);
        uids1.add(uid3);
        List<IdentityDTO> result1 = identityRepository.findIdentitiesByUidsNormalised(uids1);
        assertEquals(3, result1.size());
        assertEquals(1, result1.stream().filter(r -> r.getUid().equals(uid1)).count());
        assertEquals(1, result1.stream().filter(r -> r.getUid().equals(uid2)).count());
        assertEquals(1, result1.stream().filter(r -> r.getUid().equals(uid3)).count());

        List<String> uids2 = new ArrayList<>();
        uids2.add(uid1);
        uids2.add(uid2);
        List<IdentityDTO> result2 = identityRepository.findIdentitiesByUidsNormalised(uids2);
        assertEquals(2, result2.size());
        assertEquals(1, result2.stream().filter(r -> r.getUid().equals(uid1)).count());
        assertEquals(1, result2.stream().filter(r -> r.getUid().equals(uid2)).count());

        List<String> uids3 = new ArrayList<>();
        uids3.add(uid1);
        uids3.add(uid4);
        List<IdentityDTO> result3 = identityRepository.findIdentitiesByUidsNormalised(uids3);
        assertEquals(1, result3.size());
        assertEquals(1, result3.stream().filter(r -> r.getUid().equals(uid1)).count());
        assertEquals(0, result3.stream().filter(r -> r.getUid().equals(uid4)).count());

        List<String> uids4 = new ArrayList<>();
        uids4.add(uid4);
        List<IdentityDTO> result4 = identityRepository.findIdentitiesByUidsNormalised(uids4);
        assertEquals(0, result4.size());
        assertEquals(0, result4.stream().filter(r -> r.getUid().equals(uid4)).count());

        List<String> uids5 = new ArrayList<>();
        List<IdentityDTO> result5 = identityRepository.findIdentitiesByUidsNormalised(uids5);
        assertEquals(0, result5.size());
    }

    private Identity createIdentity() {
        return createIdentity(null);
    }

    private Identity createIdentity(String agencyTokenUid) {
        return createIdentity(UUID.randomUUID().toString(), UUID.randomUUID().toString(), agencyTokenUid);
    }

    private Identity createIdentity(String uid, String emailPrefix, String agencyTokenUid) {
        return new Identity(uid, String.format(EMAIL_TEMPLATE, emailPrefix), PASSWORD, true, false, new HashSet<>(), Instant.now(), false, agencyTokenUid);
    }
}
