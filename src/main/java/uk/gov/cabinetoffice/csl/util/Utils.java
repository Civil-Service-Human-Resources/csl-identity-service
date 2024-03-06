package uk.gov.cabinetoffice.csl.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class Utils {

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
        } catch (Exception e) {
            log.warn("Invalid date string value: {}, Exception: {}", localDateTime, e.toString());
        }
        return "";
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
    }
}
