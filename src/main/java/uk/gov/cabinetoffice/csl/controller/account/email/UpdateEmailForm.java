package uk.gov.cabinetoffice.csl.controller.account.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;

@Data
@FieldMatch(first = "email", second = "confirm", message = "{validation.updateEmail.email.FieldMatch}")
public class UpdateEmailForm {
    @NotBlank(message = "{validation.updateEmail.email.NotBlank}")
    @Email(message = "{validation.updateEmail.email.Email}")
    private String email;

    @NotBlank(message = "{validation.updateEmail.confirm.NotBlank}")
    private String confirm;
}
