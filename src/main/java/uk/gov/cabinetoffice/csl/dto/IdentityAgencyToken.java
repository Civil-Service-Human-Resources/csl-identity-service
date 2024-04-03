package uk.gov.cabinetoffice.csl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IdentityAgencyToken {
    private String uid;
    private String agencyTokenUid;
}
