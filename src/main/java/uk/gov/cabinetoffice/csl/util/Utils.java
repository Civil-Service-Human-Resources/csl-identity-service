package uk.gov.cabinetoffice.csl.util;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    public String convertDateTimeFormat(String localDateTime) {
        try {
            SimpleDateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat targetDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            Date parsedDate = sourceDateFormat.parse(localDateTime);
            return targetDateFormat.format(parsedDate);
        } catch (Exception ignored) {}
        return "";
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
    }
}
