package uk.gov.cabinetoffice.csl.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reactivation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.Month.FEBRUARY;
import static org.apache.commons.lang3.RandomStringUtils.random;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class ReactivationRepositoryTest {

    @Autowired
    private ReactivationRepository reactivationRepository;

    @Test
    public void shouldReturnPendingReactivations() {

        LocalDateTime dateOfReactivationRequest1 = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        LocalDateTime dateOfReactivationRequest2 = LocalDateTime.of(2024, FEBRUARY, 2, 11, 30);

        String email = "my.name@myorg.gov.uk";

        String code1 = random(40, true, true);
        Reactivation reactivation1 = new Reactivation();
        reactivation1.setCode(code1);
        reactivation1.setReactivationStatus(PENDING);
        reactivation1.setRequestedAt(dateOfReactivationRequest1);
        reactivation1.setEmail(email);

        String code2 = random(40, true, true);
        Reactivation reactivation2 = new Reactivation();
        reactivation2.setCode(code2);
        reactivation2.setReactivationStatus(PENDING);
        reactivation2.setRequestedAt(dateOfReactivationRequest2);
        reactivation2.setEmail(email);

        reactivationRepository.save(reactivation1);
        reactivationRepository.save(reactivation2);

        Optional<Reactivation> r1Optional = reactivationRepository.findFirstByCodeAndReactivationStatus(code1, PENDING);
        assertTrue(r1Optional.isPresent());
        Reactivation r1 = r1Optional.get();
        assertEquals(code1, r1.getCode());
        assertEquals(PENDING, r1.getReactivationStatus());
        assertEquals(dateOfReactivationRequest1, r1.getRequestedAt());

        Optional<Reactivation> r2Optional = reactivationRepository.findFirstByCodeAndReactivationStatus(code2, PENDING);
        assertTrue(r2Optional.isPresent());
        Reactivation r2 = r2Optional.get();
        assertEquals(code2, r2.getCode());
        assertEquals(PENDING, r2.getReactivationStatus());
        assertEquals(dateOfReactivationRequest2, r2.getRequestedAt());

        Optional<Reactivation> r3Optional = reactivationRepository.findFirstByEmailIgnoreCaseAndReactivationStatus(email, PENDING);
        assertTrue(r3Optional.isPresent());
        Reactivation r3 = r3Optional.get();
        assertEquals(email, r3.getEmail());
        assertEquals(PENDING, r3.getReactivationStatus());
        assertEquals(dateOfReactivationRequest1, r3.getRequestedAt());

        List<Reactivation> pendingReactivation =
                reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(email, PENDING);
        assertEquals(2, pendingReactivation.size());
    }

    @Test
    public void shouldNotReturnPendingReactivations() {
        String email = "my.name2@myorg.gov.uk";
        String code = random(40, true, true);

        Optional<Reactivation> r2 = reactivationRepository.findFirstByCodeAndReactivationStatus(code, PENDING);
        assertFalse(r2.isPresent());

        List<Reactivation> pendingReactivation =
                reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(email, PENDING);
        assertEquals(0, pendingReactivation.size());
    }
}
