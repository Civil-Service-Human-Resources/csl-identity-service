package uk.gov.cabinetoffice.csl.controller.reset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResetForm {
    private String password;
    private String confirmPassword;
    private String code;
}
