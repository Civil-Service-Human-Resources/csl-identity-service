package uk.gov.cabinetoffice.csl.exception;

public class GenericServerException extends RuntimeException {
    public GenericServerException(String message) {
        super(message);
    }
}
