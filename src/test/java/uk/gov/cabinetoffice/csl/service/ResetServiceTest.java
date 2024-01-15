package uk.gov.cabinetoffice.csl.service;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.domain.ResetStatus;
import uk.gov.cabinetoffice.csl.repository.ResetRepository;
import uk.gov.service.notify.NotificationClientException;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("no-redis")
public class ResetServiceTest {

    public static final String EMAIL = "test@example.com";
    public static final String CODE = "abc123";
    public static final String TEMPLATE_ID = "template123";
    public static final String URL = "localhost:8080";

    @InjectMocks
    private ResetService resetService;

    @Mock
    private ResetRepository resetRepository;

    @Mock
    private NotifyService notifyService;

    @Test
    public void shouldSaveNewResetWhenCreateNewResetForEmail() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        resetService.notifyForResetRequest(EMAIL);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset reset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(reset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(reset.getResetStatus(), equalTo(ResetStatus.PENDING));
    }

    @Test
    public void shouldModifyExistingResetWhenResetSuccessFor() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset expectedReset = new Reset();
        expectedReset.setEmail(EMAIL);
        expectedReset.setResetStatus(ResetStatus.PENDING);

        resetService.notifyOfSuccessfulReset(expectedReset);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset actualReset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(actualReset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(actualReset.getResetStatus(), equalTo(ResetStatus.RESET));
    }

    @Test
    public void isResetExpiredShouldReturnExpiredIfRequestedAtMoreThan24H() {
        Reset reset = new Reset();
        reset.setResetStatus(ResetStatus.PENDING);
        reset.setCode(CODE);
        reset.setRequestedAt(new Date(2323223232L));

        MatcherAssert.assertThat(resetService.isResetExpired(reset), equalTo(true));

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);
        verify(resetRepository).save(resetArgumentCaptor.capture());
        Reset actualReset = resetArgumentCaptor.getValue();

        MatcherAssert.assertThat(actualReset.getResetStatus(), equalTo(ResetStatus.EXPIRED));
    }
}
