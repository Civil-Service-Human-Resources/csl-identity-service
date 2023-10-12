package uk.gov.cabinetoffice.csl.controller.signup;

import lombok.Data;

@Data
public class EnterTokenForm {
    private String organisation;
    private String token;
    private boolean removeUser;
}
