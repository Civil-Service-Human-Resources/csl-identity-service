package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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

@Slf4j
@Service
@Transactional
public class ResetService {

    @Value("${govNotify.template.reset}")
    private String govNotifyResetTemplateId;

    @Value("${govNotify.template.resetSuccessful}")
    private String govNotifySuccessfulResetTemplateId;

    @Value("${reset.url}")
    private String resetUrlFormat;

    @Value("${reset.validityInSeconds}")
    private int validityInSeconds;

    private final ResetRepository resetRepository;

    private final NotifyService notifyService;

    @Autowired
    public ResetService(ResetRepository resetRepository, @Qualifier("notifyServiceImpl") NotifyService notifyService) {
        this.resetRepository = resetRepository;
        this.notifyService = notifyService;
    }

    public boolean isResetExpired(Reset reset) {
        long diffInMs = new Date().getTime() - reset.getRequestedAt().getTime();

        if (diffInMs > validityInSeconds * 1000L && reset.getResetStatus().equals(ResetStatus.PENDING)) {
            reset.setResetStatus(ResetStatus.EXPIRED);
            resetRepository.save(reset);
            return true;
        }

        return false;
    }

    public boolean isResetPending(Reset reset) {
        return reset.getResetStatus().equals(ResetStatus.PENDING);
    }

    public void notifyForResetRequest(String email) throws NotificationClientException {
        Reset reset = new Reset();
        reset.setEmail(email);
        reset.setRequestedAt(new Date());
        reset.setResetStatus(ResetStatus.PENDING);
        reset.setCode(RandomStringUtils.random(40, true, true));

        notifyService.notify(reset.getEmail(), reset.getCode(), govNotifyResetTemplateId, resetUrlFormat);

        resetRepository.save(reset);

        log.info("Reset request sent to {} ", email);
    }

    public void notifyOfSuccessfulReset(Reset reset) throws NotificationClientException {
        reset.setResetAt(new Date());
        reset.setResetStatus(ResetStatus.RESET);

        notifyService.notify(reset.getEmail(), reset.getCode(), govNotifySuccessfulResetTemplateId, resetUrlFormat);

        resetRepository.save(reset);

        log.info("Reset success sent to {} ", reset.getEmail());
    }
}
