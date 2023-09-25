package uk.gov.cabinetoffice.csl.exception;

public class IdentityNotFoundException extends RuntimeException {
    public IdentityNotFoundException(String message) {
        super(message);
    }
}
