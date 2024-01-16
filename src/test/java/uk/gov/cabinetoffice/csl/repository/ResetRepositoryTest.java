package uk.gov.cabinetoffice.csl.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.domain.ResetStatus;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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

    private Reset createReset() {
        Reset reset = new Reset();
        reset.setCode(CODE);
        reset.setEmail(EMAIL);
        reset.setResetStatus(ResetStatus.PENDING);
        reset.setRequestedAt(new Date());
        return reset;
    }
}
