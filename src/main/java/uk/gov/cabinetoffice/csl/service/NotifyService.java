package uk.gov.cabinetoffice.csl.service;

import uk.gov.service.notify.NotificationClientException;
import java.util.Map;

public interface NotifyService {

    void notify(String email, String code, String templateId, String actionUrl) throws NotificationClientException;

    void notify(String email, String templateId);

    void notifyWithPersonalisation(String email, String templateId, Map<String, String> personalisation);
}
