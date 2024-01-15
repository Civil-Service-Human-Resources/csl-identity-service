package uk.gov.cabinetoffice.csl.controller.reset;

import lombok.Data;

@Data
public class ResetForm {
    private String password;
    private String confirmPassword;
    private String code;
}
