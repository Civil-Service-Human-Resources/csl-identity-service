package uk.gov.cabinetoffice.csl.exception;

public class ClientAuthenticationErrorException extends RuntimeException {
    public ClientAuthenticationErrorException(String message) {
        super(message);
    }
}
