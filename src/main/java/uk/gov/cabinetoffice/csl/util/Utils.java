package uk.gov.cabinetoffice.csl.util;

public class Utils {

    public static String validityMessage(String validityMessage, long validityInSeconds) {
        long hours = validityInSeconds / 3600;
        if(hours < 1) {
            long minutes = (validityInSeconds % 3600) / 60;
            validityMessage = validityMessage.formatted(String.format("%02d", minutes) + " minutes.");
        } else {
            validityMessage = validityMessage.formatted(String.format("%02d", hours) + " hours.");
        }
        return validityMessage;
    }
}
