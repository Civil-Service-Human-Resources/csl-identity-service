package uk.gov.cabinetoffice.csl.controller.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;

@FieldMatch(first = "email", second = "confirmEmail", message = "{validation.confirmEmail.match}")
@Data
public class RequestInviteForm {
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.confirmEmail.blank}")
    private String confirmEmail;
}
