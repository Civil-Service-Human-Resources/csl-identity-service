package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.domain.ResetStatus;
import uk.gov.cabinetoffice.csl.repository.ResetRepository;
import uk.gov.service.notify.NotificationClientException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.time.Month.FEBRUARY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.*;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.PENDING;

@SpringBootTest
@ActiveProfiles("no-redis")
public class ResetServiceTest {

    public static final String EMAIL = "test@example.com";
    public static final String CODE = "abc123";
    public static final String TEMPLATE_ID = "template123";
    public static final String URL = "localhost:8080";
    private final int validityInSeconds = 86400;
    private final Clock clock = Clock.fixed(Instant.parse("2024-03-01T00:00:00Z"), ZoneId.of("Europe/London"));

    private final ResetRepository resetRepository = mock(ResetRepository.class);

    private final NotifyService notifyService = mock(NotifyService.class);

    private final ResetService resetService = new ResetService(resetRepository, notifyService, clock, validityInSeconds);

    @Test
    public void shouldCreateNewPendingReset() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        resetService.createPendingResetRequestAndAndNotifyUser(EMAIL);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository, times(1)).save(resetArgumentCaptor.capture());

        Reset reset = resetArgumentCaptor.getValue();
        assertThat(reset.getCode(), not(equalTo(CODE)));
        assertThat(reset.getEmail(), equalTo(EMAIL));
        assertThat(reset.getResetStatus(), equalTo(PENDING));
        assertNotNull(reset.getRequestedAt());
    }

    @Test
    public void updatePendingResetShouldNotUpdateEmailStatusAndCode() throws NotificationClientException {
        Reset reset = createReset();

        doNothing().when(notifyService).notify(reset.getEmail(), reset.getCode(), TEMPLATE_ID, URL);
        resetService.updatePendingResetRequestAndAndNotifyUser(reset);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository, times(1)).save(resetArgumentCaptor.capture());

        Reset reset1 = resetArgumentCaptor.getValue();
        assertThat(reset1.getCode(), equalTo(reset.getCode()));
        assertThat(reset1.getEmail(), equalTo(reset.getEmail()));
        assertThat(reset1.getResetStatus(), equalTo(PENDING));
        assertNotNull(reset1.getRequestedAt());
    }

    @Test
    public void shouldUpdateStatusOfAllExistingResetsToExpiredAndReturnNull() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset reset1 = createReset();
        Reset reset2 = createReset();
        List<Reset> existingPendingResets = new ArrayList<>();
        existingPendingResets.add(reset1);
        existingPendingResets.add(reset2);

        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        Reset reset = resetService.getPendingResetForEmail(EMAIL);
        assertNull(reset);

        verify(resetRepository, times(1)).saveAll(existingPendingResets);
        ResetStatus resetStatus1 = existingPendingResets.get(0).getResetStatus();
        assertThat(resetStatus1, equalTo(EXPIRED));
        ResetStatus resetStatus2 = existingPendingResets.get(0).getResetStatus();
        assertThat(resetStatus2, equalTo(EXPIRED));
    }

    @Test
    public void shouldUpdateStatusOfPendingResetToExpiredAndReturnNull() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset reset = createReset();

        List<Reset> existingPendingResets = new ArrayList<>();
        existingPendingResets.add(reset);

        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        Reset reset1 = resetService.getPendingResetForEmail(EMAIL);
        assertNull(reset1);
        ResetStatus resetStatus1 = existingPendingResets.get(0).getResetStatus();
        assertThat(resetStatus1, equalTo(EXPIRED));

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);
        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset actualReset = resetArgumentCaptor.getValue();

        assertThat(actualReset.getResetStatus(), equalTo(EXPIRED));

        verify(resetRepository, times(1)).save(reset);
    }

    @Test
    public void shouldReturnPendingReset() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset reset = createReset();
        reset.setRequestedAt(LocalDateTime.now());

        List<Reset> existingPendingResets = new ArrayList<>();
        existingPendingResets.add(reset);

        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        Reset reset1 = resetService.getPendingResetForEmail(EMAIL);
        ResetStatus resetStatus1 = reset1.getResetStatus();
        assertThat(resetStatus1, equalTo(PENDING));
        assertThat(reset1.getCode(), equalTo(reset.getCode()));
        assertThat(reset1.getEmail(), equalTo(reset.getEmail()));
        assertThat(reset1.getRequestedAt(), equalTo(reset.getRequestedAt()));

        verify(resetRepository, times(0)).saveAll(existingPendingResets);
    }

    @Test
    public void shouldModifyExistingResetWhenResetSuccessFor() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset expectedReset = createReset();

        resetService.notifyUserForSuccessfulReset(expectedReset);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset actualReset = resetArgumentCaptor.getValue();
        assertThat(actualReset.getCode(), equalTo(CODE));
        assertThat(actualReset.getEmail(), equalTo(EMAIL));
        assertThat(actualReset.getResetStatus(), equalTo(RESET));
    }

    @Test
    public void isResetExpiredShouldReturnExpiredIfRequestedAtMoreThan24H() {
        Reset reset = createReset();

        LocalDateTime requestDateTime = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        reset.setRequestedAt(requestDateTime);

        assertThat(resetService.isResetExpired(reset), equalTo(true));

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);
        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset actualReset = resetArgumentCaptor.getValue();
        assertThat(actualReset.getCode(), equalTo(CODE));
        assertThat(actualReset.getEmail(), equalTo(EMAIL));
        assertThat(actualReset.getResetStatus(), equalTo(EXPIRED));
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
