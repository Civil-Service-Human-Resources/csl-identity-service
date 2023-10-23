package uk.gov.cabinetoffice.csl.controller.signup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;

@Data
@FieldMatch(first = "password", second = "confirmPassword", message = "{validation.signup.password.NotMatching}")
public class SignupForm {

    @NotBlank(message = "{validation.signup.password.NotBlank}")
    @Pattern(regexp = "^(?:(?=.*[a-z])(?:(?=.*[A-Z])(?=.*[\\d\\W]))).{8,}$", message = "{validation.signup.password.MatchesPolicy}")
    private String password;

    @NotBlank(message = "{validation.signup.password.NotBlank}")
    @Pattern(regexp = "^(?:(?=.*[a-z])(?:(?=.*[A-Z])(?=.*[\\d\\W]))).{8,}$", message = "{validation.signup.password.MatchesPolicy}")
    private String confirmPassword;
}
