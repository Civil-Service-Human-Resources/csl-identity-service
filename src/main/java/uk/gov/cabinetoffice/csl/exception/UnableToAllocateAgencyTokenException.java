package uk.gov.cabinetoffice.csl.exception;

public class UnableToAllocateAgencyTokenException extends RuntimeException {
    public UnableToAllocateAgencyTokenException(String message) {
        super(message);
    }
}
