package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.service.PasswordService;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;
import uk.gov.cabinetoffice.csl.validation.annotation.IsCurrentPassword;

@Component
@AllArgsConstructor
public class IsCurrentPasswordValidator implements ConstraintValidator<IsCurrentPassword, String> {

   private final PasswordService passwordService;
   private final IUserAuthService userAuthService;

   public void initialize(IsCurrentPassword constraint) {}

   public boolean isValid(String value, ConstraintValidatorContext context) {
      Identity identity = userAuthService.getIdentity();
      return passwordService.checkPassword(identity.getEmail(), value);
   }
}
