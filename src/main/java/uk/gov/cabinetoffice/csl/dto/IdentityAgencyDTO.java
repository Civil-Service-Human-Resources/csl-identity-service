package uk.gov.cabinetoffice.csl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IdentityAgencyDTO {
    private String uid;
    private String agencyTokenUid;
}
