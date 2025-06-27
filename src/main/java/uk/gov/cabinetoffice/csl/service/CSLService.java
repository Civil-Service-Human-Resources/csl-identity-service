package uk.gov.cabinetoffice.csl.service;

import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.service.client.csl.ICSLServiceClient;

@Service
public class CSLService {
    private final ICSLServiceClient cslServiceClient;

    public CSLService(ICSLServiceClient cslServiceClient){
        this.cslServiceClient = cslServiceClient;
    }

    public void activateUser(String uid){
        cslServiceClient.activateUser(uid);
    }
}
