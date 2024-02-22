package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.exception.FieldMatchException;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class FieldMatchValidatorTest {

    private final FieldMatchValidator validator = new FieldMatchValidator();

    @BeforeAll
    static void setup() {
        mockStatic(BeanUtils.class);
    }

    @Test
    public void shouldReturnTrueIfFieldsMatch()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        assertTrue(executeValidatorToReturnBoolean("match", "match"));
    }

    @Test
    public void shouldReturnFalseIfFieldsDoNotMatch()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        assertFalse(executeValidatorToReturnBoolean("match", "no match"));
    }

    @Test
    public void shouldThrowFieldMatchExceptionOnIllegalAccessException()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        executeValidatorToThrowException(mock(IllegalAccessException.class));
    }

    @Test
    public void shouldThrowFieldMatchExceptionOnNoSuchMethodException()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        executeValidatorToThrowException(mock(NoSuchMethodException.class));
    }

    @Test
    public void shouldThrowFieldMatchExceptionOnInvocationTargetException()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        executeValidatorToThrowException(mock(InvocationTargetException.class));
    }

    private boolean executeValidatorToReturnBoolean(String value1, String value2)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        when(BeanUtils.getProperty(bean, "first")).thenReturn(value1);
        when(BeanUtils.getProperty(bean, "second")).thenReturn(value2);

        validator.initialize(fieldMatch);
        return validator.isValid(bean, constraintValidatorContext);
    }

    private <T> void executeValidatorToThrowException(T exception) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        when(BeanUtils.getProperty(bean, "first")).thenThrow((Throwable) exception);
        validator.initialize(fieldMatch);

        try {
            validator.isValid(bean, constraintValidatorContext);
            fail("Expected FieldMatchException");
        } catch (FieldMatchException e) {
            assertEquals(exception, e.getCause());
        }
    }
}
