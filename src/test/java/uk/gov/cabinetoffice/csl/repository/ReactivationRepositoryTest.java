package uk.gov.cabinetoffice.csl.repository;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reactivation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class ReactivationRepositoryTest {

    @Autowired
    private ReactivationRepository reactivationRepository;

    @Test
    public void existsReactivationByEmailAndRequestedAtBeforeReturnsTrueIfReactivationRequestExistsForEmailBeforeGivenDate() throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("14-Nov-2022");
        Date dateBeforeReactivationRequest = formatter.parse("12-Nov-2022");
        Date dateOfReactivationRequest2 = formatter.parse("15-Nov-2022");

        String email = "my.name@myorg.gov.uk";
        String code1 = RandomStringUtils.random(40, true, true);

        Reactivation reactivation = new Reactivation();
        reactivation.setCode(code1);
        reactivation.setReactivationStatus(PENDING);
        reactivation.setRequestedAt(dateOfReactivationRequest);
        reactivation.setEmail(email);

        String code2 = RandomStringUtils.random(40, true, true);
        Reactivation reactivation2 = new Reactivation();
        reactivation2.setCode(code2);
        reactivation2.setReactivationStatus(PENDING);
        reactivation2.setRequestedAt(dateOfReactivationRequest2);
        reactivation2.setEmail(email);

        reactivationRepository.save(reactivation);
        reactivationRepository.save(reactivation2);

        boolean reactivationPending = reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(email, PENDING, dateBeforeReactivationRequest);

        assertThat(reactivationPending, equalTo(true));
    }

    @Test
    public void existsReactivationByEmailAndRequestedAtBeforeReturnsFalseIfReactivationRequestDoesNotExistForEmailBeforeGivenDate() throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("14-Nov-2022");
        Date dateAfterReactivationRequest = formatter.parse("15-Nov-2022");
        String email = "my.name@myorg.gov.uk";

        Reactivation reactivation = new Reactivation();
        reactivation.setCode(RandomStringUtils.random(40, true, true));
        reactivation.setReactivationStatus(PENDING);
        reactivation.setRequestedAt(dateOfReactivationRequest);
        reactivation.setEmail(email);

        reactivationRepository.save(reactivation);

        boolean reactivationPending = reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(email, PENDING, dateAfterReactivationRequest);

        assertThat(reactivationPending, equalTo(false));
    }

    @Test
    public void existsByEmailAndReactivationStatusEqualsReturnsFalseIfReactivationDoesNotExistsForEmailAndReactivationStatus() throws ParseException {
        String email = "my.name2@myorg.gov.uk";
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        Date date = formatter.parse("12-Nov-2022");

        boolean pendingReactivationExists = reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(email, PENDING, date);

        assertThat(pendingReactivationExists, equalTo(false));
    }
}
