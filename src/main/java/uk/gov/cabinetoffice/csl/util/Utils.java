package uk.gov.cabinetoffice.csl.util;

import org.springframework.stereotype.Component;

@Component
public class Utils {

    public String validityMessage(String validityMessage, long validityInSeconds) {
        long hours = validityInSeconds / 3600;
        if(hours < 1) {
            long minutes = (validityInSeconds % 3600) / 60;
            validityMessage = validityMessage.formatted(String.format("%02d", minutes) + " minutes");
        } else {
            validityMessage = validityMessage.formatted(String.format("%02d", hours) + " hours");
        }
        return validityMessage;
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
    }
}
