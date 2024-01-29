package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.service.UserService;
import uk.gov.cabinetoffice.csl.validation.annotation.IsCurrentPassword;

@Component
public class IsCurrentPasswordValidator implements ConstraintValidator<IsCurrentPassword, String> {
   private final UserService userService;
   private final AuthenticationDetailsService authenticationDetailsService;

   public IsCurrentPasswordValidator(UserService userService, AuthenticationDetailsService authenticationDetailsService) {
      this.userService = userService;
      this.authenticationDetailsService = authenticationDetailsService;
   }

   public void initialize(IsCurrentPassword constraint) {
   }

   public boolean isValid(String value, ConstraintValidatorContext context) {
      Identity identity = authenticationDetailsService.getCurrentIdentity();

      return userService.checkPassword(identity.getEmail(), value);
   }
}
