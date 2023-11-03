package uk.gov.cabinetoffice.csl.validation.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.cabinetoffice.csl.exception.FieldMatchException;
import uk.gov.cabinetoffice.csl.validation.annotation.FieldMatch;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FieldMatchValidatorTest {

    private final FieldMatchValidator validator = new FieldMatchValidator();

    @BeforeAll
    static void setup() {
        mockStatic(BeanUtils.class);
    }

    @Test
    public void shouldReturnTrueIfFieldsMatch() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        when(BeanUtils.getProperty(bean, "first")).thenReturn("match");
        when(BeanUtils.getProperty(bean, "second")).thenReturn("match");

        validator.initialize(fieldMatch);
        assertTrue(validator.isValid(bean, constraintValidatorContext));
    }

    @Test
    public void shouldReturnFalseIfFieldsDoNotMatch() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        when(BeanUtils.getProperty(bean, "first")).thenReturn("match");
        when(BeanUtils.getProperty(bean, "second")).thenReturn("no match");

        validator.initialize(fieldMatch);
        assertFalse(validator.isValid(bean, constraintValidatorContext));
    }


    @Test
    public void shouldThrowFieldMatchExceptionOnIllegalAccessException()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        IllegalAccessException exception = mock(IllegalAccessException.class);

        when(BeanUtils.getProperty(bean, "first")).thenThrow(exception);
        validator.initialize(fieldMatch);

        try {
            validator.isValid(bean, constraintValidatorContext);
            fail("Expected FieldMatchException");
        } catch (FieldMatchException e) {
            assertEquals(exception, e.getCause());
        }
    }

    @Test
    public void shouldThrowFieldMatchExceptionOnNoSuchMethodException()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        NoSuchMethodException exception = mock(NoSuchMethodException.class);

        when(BeanUtils.getProperty(bean, "first")).thenThrow(exception);
        validator.initialize(fieldMatch);

        try {
            validator.isValid(bean, constraintValidatorContext);
            fail("Expected FieldMatchException");
        } catch (FieldMatchException e) {
            assertEquals(exception, e.getCause());
        }
    }

    @Test
    public void shouldThrowFieldMatchExceptionOnInvocationTargetException()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConstraintValidatorContext constraintValidatorContext = mock(ConstraintValidatorContext.class);
        FieldMatch fieldMatch = mock(FieldMatch.class);

        when(fieldMatch.first()).thenReturn("first");
        when(fieldMatch.second()).thenReturn("second");

        Object bean = new Object();

        InvocationTargetException exception = mock(InvocationTargetException.class);

        when(BeanUtils.getProperty(bean, "first")).thenThrow(exception);
        validator.initialize(fieldMatch);

        try {
            validator.isValid(bean, constraintValidatorContext);
            fail("Expected FieldMatchException");
        } catch (FieldMatchException e) {
            assertEquals(exception, e.getCause());
        }
    }
}
