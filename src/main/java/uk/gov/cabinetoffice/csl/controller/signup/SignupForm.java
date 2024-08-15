package uk.gov.cabinetoffice.csl.controller.signup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;
import uk.gov.cabinetoffice.csl.validation.annotation.MatchesPolicy;

@Data
@FieldMatch(first = "password", second = "confirmPassword", message = "{validation.signup.password.NotMatching}")
public class SignupForm {

    @NotBlank(message = "{validation.signup.password.NotBlank}")
    @MatchesPolicy(message = "{validation.signup.password.MatchesPolicy}")
    private String password;

    @NotBlank(message = "{validation.signup.password.NotBlank}")
    @MatchesPolicy(message = "{validation.signup.password.MatchesPolicy}")
    private String confirmPassword;
}
