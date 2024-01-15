package uk.gov.cabinetoffice.csl.controller.reset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ResetFormValidator implements Validator {

    private final String passwordPattern;

    @Autowired
    public ResetFormValidator(@Value("${accountValidation.passwordPattern}") String passwordPattern) {
        this.passwordPattern = passwordPattern;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ResetForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ResetForm form = (ResetForm) target;
        if (form.getPassword() == null || !form.getPassword().matches(passwordPattern)) {
            errors.rejectValue("password", "validation.password");
        }
        if (form.getConfirmPassword() == null || !form.getConfirmPassword().equals(form.getPassword())) {
            errors.rejectValue("confirmPassword", "validation.confirmPassword");
        }
    }
}
