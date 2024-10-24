package uk.gov.cabinetoffice.csl.controller.reset;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.PasswordService;
import uk.gov.cabinetoffice.csl.service.ResetService;
import uk.gov.service.notify.NotificationClientException;

import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CONTACT_EMAIL_ATTRIBUTE;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CONTACT_NUMBER_ATTRIBUTE;

@Slf4j
@Controller
@RequestMapping("/reset")
public class ResetController {

    private static final String REQUEST_RESET_TEMPLATE = "reset/requestReset";
    private static final String CHECK_EMAIL_TEMPLATE = "reset/checkEmail";
    private static final String PENDING_RESET_TEMPLATE = "reset/pendingReset";
    private static final String PASSWORD_FORM_TEMPLATE = "reset/passwordForm";
    private static final String PASSWORD_RESET_TEMPLATE = "reset/passwordReset";

    @Value("${lpg.uiSignOutUrl}")
    private String lpgUiSignOutUrl;

    @Value("${lpg.contactEmail}")
    private String contactEmail;

    @Value("${lpg.contactNumber}")
    private String contactNumber;

    private final ResetService resetService;

    private final PasswordService passwordService;

    private final IdentityService identityService;

    private final ResetFormValidator resetFormValidator;

    public ResetController(ResetService resetService, PasswordService passwordService,
                           IdentityService identityService, ResetFormValidator resetFormValidator) {
        this.resetService = resetService;
        this.passwordService = passwordService;
        this.identityService = identityService;
        this.resetFormValidator = resetFormValidator;
    }

    @GetMapping
    public String reset() {
        return REQUEST_RESET_TEMPLATE;
    }

    @PostMapping
    public String requestReset(@RequestParam(value = "email") String email, Model model)
            throws NotificationClientException {
        log.debug("Reset request received for email {}", email);
        model.addAttribute("resetEmailId", email);
        model.addAttribute(CONTACT_EMAIL_ATTRIBUTE, contactEmail);
        model.addAttribute(CONTACT_NUMBER_ATTRIBUTE, contactNumber);
        if (identityService.isIdentityExistsForEmail(email)) {
            Reset pendingReset = resetService.getPendingResetForEmail(email);
            if(pendingReset == null) {
                resetService.createPendingResetRequestAndAndNotifyUser(email);
                log.info("Reset request email sent to {}", email);
                return CHECK_EMAIL_TEMPLATE;
            } else {
                log.info("Pending Reset exists for email {}", email);
                return PENDING_RESET_TEMPLATE;
            }
        } else {
            log.info("Identity does not exist for {} therefore Reset request is not sent.", email);
            return CHECK_EMAIL_TEMPLATE;
        }
    }

    @GetMapping("/{code}")
    public String loadResetForm(@PathVariable(value = "code") String code, Model model) {
        log.debug("User on reset screen with code {}", code);

        Reset reset = resetService.getResetForCode(code);
        String checkResetValidityResult = checkResetValidity(reset, code, model);

        if(StringUtils.isBlank(checkResetValidityResult)) {
            ResetForm resetForm = new ResetForm();
            resetForm.setCode(code);
            model.addAttribute("resetForm", resetForm);
            return PASSWORD_FORM_TEMPLATE;
        }

        return checkResetValidityResult;
    }

    @PostMapping("/{code}")
    public String resetPassword(@PathVariable(value = "code") String code,
                                @ModelAttribute @Valid ResetForm resetForm,
                                BindingResult bindingResult, Model model)
            throws NotificationClientException {
        log.debug("User on enter password screen for reset code {}", code);

        if (bindingResult.hasErrors()) {
            model.addAttribute("resetForm", resetForm);
            return PASSWORD_FORM_TEMPLATE;
        }

        Reset reset = resetService.getResetForCode(code);
        String result = checkResetValidity(reset, code, model);

        if(StringUtils.isBlank(result)) {
            Identity identity = identityService.getIdentityForEmail(reset.getEmail());

            if (identity == null || identity.getEmail() == null) {
                log.info("Identity does not exist for email {} which is retrieved using the reset code {}",
                        reset.getEmail(), code);
                model.addAttribute("userMessage", "The reset link is invalid.\n" +
                        "Please submit the reset request for the valid email id.");
                return REQUEST_RESET_TEMPLATE;
            }

            passwordService.updatePasswordAndActivateAndUnlock(identity, resetForm.getPassword());
            resetService.notifyUserForSuccessfulReset(reset);
            log.info("Reset success sent to {}", reset.getEmail());
            model.addAttribute("lpgUiSignOutUrl", lpgUiSignOutUrl);
            return PASSWORD_RESET_TEMPLATE;
        }
        return result;
    }

    @InitBinder
    public void resetValidation(WebDataBinder binder) {
        if (binder.getTarget() instanceof ResetForm) {
            binder.addValidators(resetFormValidator);
        }
    }

    private String checkResetValidity(Reset reset, String code, Model model) {

        if (StringUtils.isBlank(code)) {
            log.info("The reset code is blank.");
            model.addAttribute("userMessage", "The reset link is invalid.\n" +
                    "Please re-submit the reset request.");
            return REQUEST_RESET_TEMPLATE;
        }

        if (reset == null || StringUtils.isBlank(reset.getEmail())) {
            log.info("The reset does not exist for the code {}", code);
            model.addAttribute("userMessage", "The reset link is invalid.\n" +
                    "Please re-submit the reset request.");
            return REQUEST_RESET_TEMPLATE;
        }

        if (resetService.isResetComplete(reset)) {
            log.info("The reset is already used for the code {}", reset.getCode());
            model.addAttribute("userMessage", "The reset link is already used.\n" +
                    "Please re-submit the reset request.");
            return REQUEST_RESET_TEMPLATE;
        }

        if (resetService.isResetExpired(reset)) {
            log.info("The reset is expired for the code {}", reset.getCode());
            model.addAttribute("userMessage", "The reset link is expired.\n" +
                    "Please re-submit the reset request.");
            return REQUEST_RESET_TEMPLATE;
        }

        return "";
    }
}
