package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.service.UserService;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("no-redis")
public class IsCurrentPasswordValidatorTest {

    @Mock
    private UserService userService;

    @Mock
    private IUserAuthService userAuthService;

    @InjectMocks
    private IsCurrentPasswordValidator validator;

    @Test
    public void shouldReturnTrueIfPasswordMatchesCurrentPassword() {
        String value = "password";
        String email = "learner@domain.com";

        Identity identity = mock(Identity.class);
        when(identity.getEmail()).thenReturn(email);
        when(userAuthService.getIdentity()).thenReturn(identity);

        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);

        when(userService.checkPassword(email, value)).thenReturn(true);

        assertTrue(validator.isValid(value, constraintValidatorContext));
    }

    @Test
    public void shouldReturnFalseIfPasswordDoesNotMatchCurrentPassword() {
        String value = "password";
        String email = "learner@domain.com";

        Identity identity = mock(Identity.class);
        when(identity.getEmail()).thenReturn(email);
        when(userAuthService.getIdentity()).thenReturn(identity);

        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);

        when(userService.checkPassword(email, value)).thenReturn(false);

        assertFalse(validator.isValid(value, constraintValidatorContext));
    }
}
