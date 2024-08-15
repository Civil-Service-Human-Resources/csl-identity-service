package uk.gov.cabinetoffice.csl.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.cabinetoffice.csl.validation.validator.MatchesPolicyValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Constraint(validatedBy = MatchesPolicyValidator.class)
public @interface MatchesPolicy {
    String message() default "{validation.password.matchesPolicy}";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
