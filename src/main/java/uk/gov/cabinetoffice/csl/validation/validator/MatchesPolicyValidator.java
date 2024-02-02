package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.validation.annotation.MatchesPolicy;

@Component
public class MatchesPolicyValidator implements ConstraintValidator<MatchesPolicy, String> {

   private final String passwordPattern;

   public MatchesPolicyValidator(@Value("${accountValidation.passwordPattern}") String passwordPattern) {
      this.passwordPattern = passwordPattern;
   }

   public void initialize(MatchesPolicy constraint) {
   }

   public boolean isValid(String value, ConstraintValidatorContext context) {
      return value.matches(passwordPattern);
   }
}
