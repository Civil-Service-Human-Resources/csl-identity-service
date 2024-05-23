package uk.gov.cabinetoffice.csl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDto;
import uk.gov.cabinetoffice.csl.util.TestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class IdentityRepositoryTest {

    public static final String EMAIL_TEMPLATE = "%s@example.org";
    public static final String PASSWORD = "password123";

    @Autowired
    private IdentityRepository identityRepository;

    @Test
    public void findByForEmailShouldReturnCorrectInvite() {
        Identity identity = createIdentity();
        identityRepository.save(identity);

        assertThat(identityRepository.existsByEmailIgnoreCase(identity.getEmail()), equalTo(true));
        assertThat(identityRepository.existsByEmailIgnoreCase("doesntexist@example.com"), equalTo(false));

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

        Identity updatedIdentity = identityRepository.getReferenceById(originalIdentity.getId());
        Identity postUpdateOtherAgencyTokenIdentity = identityRepository.getReferenceById(otherAgencyTokenIdentity.getId());
        Identity postUpdateOtherNonAgencyIdentity = identityRepository.getReferenceById(otherNonAgencyIdentity.getId());

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

        Identity updatedIdentityOne = identityRepository.getReferenceById(originalIdentityOne.getId());
        Identity updatedIdentityTwo = identityRepository.getReferenceById(originalIdentityTwo.getId());
        Identity postUpdateOtherAgencyTokenIdentity = identityRepository.getReferenceById(otherAgencyTokenIdentity.getId());
        Identity postUpdateOtherNonAgencyIdentity = identityRepository.getReferenceById(otherNonAgencyIdentity.getId());

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

        Identity updatedIdentity = identityRepository.getReferenceById(originalIdentity.getId());
        Identity postUpdateOtherAgencyTokenIdentity = identityRepository.getReferenceById(otherAgencyTokenIdentity.getId());
        Identity postUpdateOtherNonAgencyIdentity = identityRepository.getReferenceById(otherNonAgencyIdentity.getId());

        assertEquals(originalIdentity.toString(), updatedIdentity.toString());
        assertEquals(otherAgencyTokenIdentity.toString(), postUpdateOtherAgencyTokenIdentity.toString());
        assertEquals(otherNonAgencyIdentity.toString(), postUpdateOtherNonAgencyIdentity.toString());
    }

    @Test
    public void findIdentitiesByUIDsNormalised_shouldReturnIdentitiesForGivenUIDs() {

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

        List<String> UIDs1 = new ArrayList<>();
        UIDs1.add(uid1);
        UIDs1.add(uid2);
        UIDs1.add(uid3);
        List<IdentityDto> result1 = identityRepository.findIdentitiesByUidsNormalised(UIDs1);
        assertEquals(3, result1.size());
        assertEquals(1, result1.stream().filter(r -> r.getUid().equals(uid1)).count());
        assertEquals(1, result1.stream().filter(r -> r.getUid().equals(uid2)).count());
        assertEquals(1, result1.stream().filter(r -> r.getUid().equals(uid3)).count());

        List<String> UIDs2 = new ArrayList<>();
        UIDs2.add(uid1);
        UIDs2.add(uid2);
        List<IdentityDto> result2 = identityRepository.findIdentitiesByUidsNormalised(UIDs2);
        assertEquals(2, result2.size());
        assertEquals(1, result2.stream().filter(r -> r.getUid().equals(uid1)).count());
        assertEquals(1, result2.stream().filter(r -> r.getUid().equals(uid2)).count());

        List<String> UIDs3 = new ArrayList<>();
        UIDs3.add(uid1);
        UIDs3.add(uid4);
        List<IdentityDto> result3 = identityRepository.findIdentitiesByUidsNormalised(UIDs3);
        assertEquals(1, result3.size());
        assertEquals(1, result3.stream().filter(r -> r.getUid().equals(uid1)).count());
        assertEquals(0, result3.stream().filter(r -> r.getUid().equals(uid4)).count());

        List<String> UIDs4 = new ArrayList<>();
        UIDs4.add(uid4);
        List<IdentityDto> result4 = identityRepository.findIdentitiesByUidsNormalised(UIDs4);
        assertEquals(0, result4.size());
        assertEquals(0, result4.stream().filter(r -> r.getUid().equals(uid4)).count());

        List<String> UIDs5 = new ArrayList<>();
        List<IdentityDto> result5 = identityRepository.findIdentitiesByUidsNormalised(UIDs5);
        assertEquals(0, result5.size());
    }

    private Identity createIdentity() {
        return createIdentity(null);
    }

    private Identity createIdentity(String agencyTokenUid) {
        return createIdentity(UUID.randomUUID().toString(), UUID.randomUUID().toString(), agencyTokenUid);
    }

    private Identity createIdentity(String uid, String emailPrefix, String agencyTokenUid) {
        return TestUtil.createIdentity(uid, String.format(EMAIL_TEMPLATE, emailPrefix), PASSWORD, agencyTokenUid);
    }
}
