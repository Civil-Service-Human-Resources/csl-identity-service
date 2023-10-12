package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.exception.NotificationException;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class NotifyServiceImpl implements NotifyService {

    private static final String EMAIL_PERMISSION = "email";
    private static final String ACTIVATION_URL_PERMISSION = "activationUrl";

    private final NotificationClient notificationClient;

    public NotifyServiceImpl(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public void notify(String email, String code, String templateId, String actionUrl) throws NotificationClientException {
        String activationUrl = String.format(actionUrl, code);

        HashMap<String, String> personalisation = new HashMap<>();
        personalisation.put(EMAIL_PERMISSION, email);
        personalisation.put(ACTIVATION_URL_PERMISSION, activationUrl);

        SendEmailResponse response = notificationClient.sendEmail(templateId, email, personalisation, "");

        log.info("Notify email sent to: {}", response.getBody());
    }

    @Override
    public void notify(String email, String templateId) {
        try {
            SendEmailResponse response =
                    notificationClient.sendEmail(templateId, email, Collections.emptyMap(), null);
            log.info("Update password notification sent to: {}", response.getBody());
        } catch (NotificationClientException e) {
            throw new NotificationException(e);
        }
    }

    @Override
    public void notifyWithPersonalisation(String email, String templateId, Map<String, String> personalisation) {
        try {
            SendEmailResponse response =
                    notificationClient.sendEmail(templateId, email, personalisation, null);
            log.info("Update email notification sent to {}, {}", email, response.getBody());
        } catch (NotificationClientException e) {
            throw new NotificationException(e);
        }
    }
}
