package uk.gov.cabinetoffice.csl.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reset;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.Month.FEBRUARY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.*;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class ResetRepositoryTest {

    public static final String CODE = "ABC123";
    public static final String EMAIL = "test@example.com";

    @Autowired
    private ResetRepository resetRepository;

    @Test
    public void existsByCodeReturnsCorrectBoolean() {
        resetRepository.save(createReset());

        assertThat(resetRepository.existsByCode(CODE), equalTo(true));
        assertThat(resetRepository.existsByCode("def567"), equalTo(false));
    }

    @Test
    public void findByCodeShouldReturnCorrectCode() {
        Reset expectedReset = createReset();
        resetRepository.save(expectedReset);
        Reset actualReset = resetRepository.findByCode(CODE);

        assertThat(actualReset.getCode(), equalTo(expectedReset.getCode()));
        assertThat(actualReset.getEmail(), equalTo(expectedReset.getEmail()));
    }

    @Test
    public void findByEmailIgnoreCaseAndResetStatusShouldReturnCorrectResetRecord() {
        Reset expectedReset = createReset();
        resetRepository.save(expectedReset);
        List<Reset> byEmailIgnoreCaseAndResetStatusIgnoreCase =
                resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING);
        Reset actualReset = byEmailIgnoreCaseAndResetStatusIgnoreCase.get(0);
        assertThat(actualReset.getResetStatus(), equalTo(expectedReset.getResetStatus()));
        assertThat(actualReset.getCode(), equalTo(expectedReset.getCode()));
        assertThat(actualReset.getEmail(), equalTo(expectedReset.getEmail()));
    }

    private Reset createReset() {
        Reset reset = new Reset();
        reset.setCode(CODE);
        reset.setEmail(EMAIL);
        reset.setResetStatus(PENDING);
        LocalDateTime requestDateTime = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        reset.setRequestedAt(requestDateTime);
        return reset;
    }
}
