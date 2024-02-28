package uk.gov.cabinetoffice.csl.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reactivation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class ReactivationRepositoryTest {

    @Autowired
    private ReactivationRepository reactivationRepository;

    @Test
    public void existsReactivationByEmailAndRequestedAtBeforeReturnsTrueIfReactivationRequestExistsForEmailBeforeGivenDate()
            throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("14-Nov-2022");
        Date dateBeforeReactivationRequest = formatter.parse("12-Nov-2022");
        Date dateOfReactivationRequest2 = formatter.parse("15-Nov-2022");

        String email = "my.name@myorg.gov.uk";
        String code1 = random(40, true, true);

        Reactivation reactivation1 = new Reactivation();
        reactivation1.setCode(code1);
        reactivation1.setReactivationStatus(PENDING);
        reactivation1.setRequestedAt(dateOfReactivationRequest);
        reactivation1.setEmail(email);

        String code2 = random(40, true, true);
        Reactivation reactivation2 = new Reactivation();
        reactivation2.setCode(code2);
        reactivation2.setReactivationStatus(PENDING);
        reactivation2.setRequestedAt(dateOfReactivationRequest2);
        reactivation2.setEmail(email);

        reactivationRepository.save(reactivation1);
        reactivationRepository.save(reactivation2);

        boolean reactivationPending = reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(
                email, PENDING, dateBeforeReactivationRequest);
        assertTrue(reactivationPending);

        Optional<Reactivation> r1 = reactivationRepository.findFirstByCodeAndReactivationStatusEquals(code1, PENDING);
        assertTrue(r1.isPresent());

        Optional<Reactivation> r2 = reactivationRepository.findFirstByCodeAndReactivationStatusEquals(code2, PENDING);
        assertTrue(r2.isPresent());
    }

    @Test
    public void existsReactivationByEmailAndRequestedAtBeforeReturnsFalseIfReactivationRequestDoesNotExistForEmailBeforeGivenDate()
            throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("14-Nov-2022");
        Date dateAfterReactivationRequest = formatter.parse("15-Nov-2022");
        String email = "my.name@myorg.gov.uk";

        Reactivation reactivation = new Reactivation();
        String code = random(40, true, true);
        reactivation.setCode(code);
        reactivation.setReactivationStatus(PENDING);
        reactivation.setRequestedAt(dateOfReactivationRequest);
        reactivation.setEmail(email);

        reactivationRepository.save(reactivation);

        boolean reactivationPending = reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(
                email, PENDING, dateAfterReactivationRequest);
        assertFalse(reactivationPending);

        Optional<Reactivation> r = reactivationRepository.findFirstByCodeAndReactivationStatusEquals(code, PENDING);
        assertTrue(r.isPresent());
    }

    @Test
    public void existsByEmailAndReactivationStatusEqualsReturnsFalseIfReactivationDoesNotExistsForEmailAndReactivationStatus()
            throws ParseException {
        String email = "my.name2@myorg.gov.uk";
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", ENGLISH);
        Date date = formatter.parse("12-Nov-2022");

        boolean pendingReactivationExists = reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(
                email, PENDING, date);
        assertFalse(pendingReactivationExists);
    }
}
