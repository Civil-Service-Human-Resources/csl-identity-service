package uk.gov.cabinetoffice.csl.controller.signup;

import lombok.Data;

@Data
public class EnterTokenWithOrganisationForm {
    private String organisation;
    private String token;
}
