package uk.gov.cabinetoffice.csl.exception;

public class VerificationCodeTypeNotFound extends RuntimeException {
    public VerificationCodeTypeNotFound(String message) {
        super(message);
    }
}
