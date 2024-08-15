package uk.gov.cabinetoffice.csl.controller.passwordupdate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;
import uk.gov.cabinetoffice.csl.validation.annotation.IsCurrentPassword;
import uk.gov.cabinetoffice.csl.validation.annotation.MatchesPolicy;

@FieldMatch(first = "newPassword", second = "confirm", message = "{validation.updatePassword.newPassword.FieldMatch}")
@Data
public class UpdatePasswordForm {
    @NotBlank(message = "{validation.updatePassword.password.NotBlank}")
    @IsCurrentPassword(message = "{validation.updatePassword.password.IsCurrentPassword}")
    private String password;

    @NotBlank(message = "{validation.updatePassword.newPassword.NotBlank}")
    @MatchesPolicy(message = "{validation.updatePassword.newPassword.MatchesPolicy}")
    private String newPassword;

    @NotBlank(message = "{validation.updatePassword.confirm.NotBlank}")
    private String confirm;
}
