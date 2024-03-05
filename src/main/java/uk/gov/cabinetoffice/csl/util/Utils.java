package uk.gov.cabinetoffice.csl.util;

import org.springframework.stereotype.Component;

@Component
public class Utils {

    public String validityMessage(String validityMessage, long validityInSeconds) {
        return validityMessage.formatted(convertSecondsIntoMinutesOrHours(validityInSeconds));
    }

    public String convertSecondsIntoMinutesOrHours(long seconds) {
        long hours = seconds / 3600;
        if(hours < 1) {
            long minutes = (seconds % 3600) / 60;
            return String.format("%02d", minutes) + " minutes";

        }
        return String.format("%02d", hours) + " hours";
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
    }
}
