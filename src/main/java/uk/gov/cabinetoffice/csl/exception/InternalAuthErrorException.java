package uk.gov.cabinetoffice.csl.exception;

public class InternalAuthErrorException extends GenericServerException {
    public InternalAuthErrorException(String message) {
        super(message);
    }
}
