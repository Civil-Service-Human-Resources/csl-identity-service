package uk.gov.cabinetoffice.csl.controller.reset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetForm {
    private String password;
    private String confirmPassword;
    private String code;
}
