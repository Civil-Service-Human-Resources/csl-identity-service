package uk.gov.cabinetoffice.csl.service.client.csl;

public interface ICSLServiceClient {
    void activateUser(String uid);
    void updateEmail(String uid, String emailId);
}
