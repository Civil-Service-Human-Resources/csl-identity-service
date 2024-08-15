package uk.gov.cabinetoffice.csl.exception;

public class PendingReactivationExistsException extends RuntimeException{
    public PendingReactivationExistsException(String message){
        super(message);
    }
}
