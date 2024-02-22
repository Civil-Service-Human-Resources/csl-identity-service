package uk.gov.cabinetoffice.csl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.domain.ResetStatus;
import uk.gov.cabinetoffice.csl.repository.ResetRepository;
import uk.gov.service.notify.NotificationClientException;

import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.EXPIRED;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.PENDING;

@Service
@Transactional
public class ResetService {

    @Value("${govNotify.template.reset}")
    private String govNotifyResetTemplateId;

    @Value("${govNotify.template.resetSuccessful}")
    private String govNotifySuccessfulResetTemplateId;

    @Value("${reset.url}")
    private String resetUrlFormat;

    private final int validityInSeconds;

    private final ResetRepository resetRepository;

    private final NotifyService notifyService;

    @Autowired
    public ResetService(ResetRepository resetRepository,
                        @Qualifier("notifyServiceImpl") NotifyService notifyService,
                        @Value("${reset.validityInSeconds}") int validityInSeconds) {
        this.resetRepository = resetRepository;
        this.notifyService = notifyService;
        this.validityInSeconds = validityInSeconds;
    }

    public Reset getResetByCode(String code) {
        return resetRepository.findByCode(code);
    }

    public boolean isResetExpired(Reset reset) {
        if(reset.getResetStatus().equals(ResetStatus.EXPIRED) || isResetComplete(reset)) {
            return true;
        }

        if(isResetPending(reset)) {
            long diffInMs = new Date().getTime() - reset.getRequestedAt().getTime();
            if(diffInMs > validityInSeconds * 1000L) {
                reset.setResetStatus(ResetStatus.EXPIRED);
                resetRepository.save(reset);
                return true;
            }
        }
        return false;
    }

    public boolean isResetPending(Reset reset) {
        return reset.getResetStatus().equals(PENDING);
    }

    public boolean isResetComplete(Reset reset) {
        return reset.getResetStatus().equals(ResetStatus.RESET);
    }

    public void notifyForResetRequest(String email) throws NotificationClientException {

        Reset reset = null;

        List<Reset> existingPendingResets =
                resetRepository.findByEmailIgnoreCaseAndResetStatus(email, PENDING);

        if(existingPendingResets != null && existingPendingResets.size() > 1) {
            existingPendingResets.forEach(r -> r.setResetStatus(EXPIRED));
            resetRepository.saveAll(existingPendingResets);
        }

        if(existingPendingResets != null && existingPendingResets.size() == 1) {
            reset = existingPendingResets.get(0);
            if(isResetExpired(reset)) {
                reset = null;
            } else {
                reset.setRequestedAt(new Date());
            }
        }

        if(reset == null) {
            reset = createPendingReset(email);
        }
        resetRepository.save(reset);
        notifyService.notify(reset.getEmail(), reset.getCode(), govNotifyResetTemplateId, resetUrlFormat);
    }

    public void notifyOfSuccessfulReset(Reset reset) throws NotificationClientException {
        reset.setResetAt(new Date());
        reset.setResetStatus(ResetStatus.RESET);
        resetRepository.save(reset);
        notifyService.notify(reset.getEmail(), reset.getCode(), govNotifySuccessfulResetTemplateId, resetUrlFormat);
    }

    private Reset createPendingReset(String email) {
        return new Reset(random(40, true, true), email, PENDING, new Date());
    }
}
