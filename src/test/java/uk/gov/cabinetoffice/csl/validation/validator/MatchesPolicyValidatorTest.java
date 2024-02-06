package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ActiveProfiles("no-redis")
public class MatchesPolicyValidatorTest {
    /*
        - 8 or more characters
        - at least 1 number
        - upper and lower case letters
     */

    private final String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";

    private final MatchesPolicyValidator validator = new MatchesPolicyValidator(passwordPattern);

    @Test
    public void shouldMatchPolicy() {
        String password = "Password123";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertTrue(validator.isValid(password, context));
    }

    @Test
    public void shouldFailMatchPolicyIfNumberMissing() {
        String password = "Password";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertFalse(validator.isValid(password, context));
    }

    @Test
    public void shouldFailMatchPolicyIfLowerCaseMissing() {
        String password = "PASSWORD123";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertFalse(validator.isValid(password, context));
    }

    @Test
    public void shouldFailMatchPolicyIfUpperCaseMissing() {
        String password = "password123";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertFalse(validator.isValid(password, context));
    }

    @Test
    public void shouldFailMatchPolicyIfCharacterMissing() {
        String password = "1234567890";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertFalse(validator.isValid(password, context));
    }

    @Test
    public void shouldFailMatchPolicyIfLessThan8Characters() {
        String password = "Passwo1";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertFalse(validator.isValid(password, context));
    }
}
