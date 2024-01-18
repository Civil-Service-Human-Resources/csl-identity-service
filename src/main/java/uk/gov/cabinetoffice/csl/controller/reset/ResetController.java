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
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.repository.ResetRepository;
import uk.gov.cabinetoffice.csl.service.ResetService;
import uk.gov.cabinetoffice.csl.service.UserService;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Controller
@RequestMapping("/reset")
public class ResetController {

    private final ResetService resetService;

    private final ResetRepository resetRepository;

    private final IdentityRepository identityRepository;

    private final UserService userService;

    private final ResetFormValidator resetFormValidator;

    private final String lpgUiUrl;

    public ResetController(ResetService resetService, UserService userService,
                           ResetRepository resetRepository, IdentityRepository identityRepository,
                           ResetFormValidator resetFormValidator,
                           @Value("${lpg.uiUrl}") String lpgUiUrl) {
        this.resetService = resetService;
        this.userService = userService;
        this.resetRepository = resetRepository;
        this.identityRepository = identityRepository;
        this.resetFormValidator = resetFormValidator;
        this.lpgUiUrl = lpgUiUrl;
    }

    @GetMapping
    public String reset() {
        return "reset/requestReset";
    }

    @PostMapping
    public String requestReset(@RequestParam(value = "email") String email, Model model) throws Exception {
        log.debug("Reset request received for email {}", email);
        if (identityRepository.existsByEmail(email)) {
            resetService.notifyForResetRequest(email);
            log.info("Reset request email sent to {}", email);
            return "reset/checkEmail";
        } else {
            log.info("Identity does not exist for {} therefore Reset request is not sent.", email);
            model.addAttribute("userMessage", "Invalid email id. Submit the reset request for the valid email id.");
            return "reset/requestReset";
        }
    }

    @GetMapping("/{code}")
    public String loadResetForm(@PathVariable(value = "code") String code, Model model) {
        log.debug("User on reset screen with code {}", code);

        Reset reset = resetRepository.findByCode(code);
        String checkResetValidityResult = checkResetValidity(reset, code, model);

        if(StringUtils.isBlank(checkResetValidityResult)) {
            ResetForm resetForm = new ResetForm();
            resetForm.setCode(code);
            model.addAttribute("resetForm", resetForm);
            return "reset/passwordForm";
        }

        return checkResetValidityResult;
    }

    @PostMapping("/{code}")
    public String resetPassword(@PathVariable(value = "code") String code,
                                @ModelAttribute @Valid ResetForm resetForm,
                                BindingResult bindingResult, Model model)
            throws NotificationClientException {

        if (bindingResult.hasErrors()) {
            model.addAttribute("resetForm", resetForm);
            return "reset/passwordForm";
        }

        Reset reset = resetRepository.findByCode(code);
        String result = checkResetValidity(reset, code, model);

        if(StringUtils.isBlank(result)) {
            Identity identity = identityRepository.findFirstByEmailEquals(reset.getEmail());

            if (identity == null || identity.getEmail() == null) {
                log.info("Identity does not exist for email {} which is retrieved using Reset code {}", reset.getEmail(), code);
                model.addAttribute("userMessage", "Invalid reset code. Submit the reset request for the valid email id.");
                return "reset/requestReset";
            }

            userService.updatePassword(identity, resetForm.getPassword());
            resetService.notifyOfSuccessfulReset(reset);
            log.info("Reset success sent to {}", reset.getEmail());
            model.addAttribute("lpgUiUrl", lpgUiUrl);
            return "reset/passwordReset";
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

        if (reset == null || reset.getEmail() == null) {
            log.info("Reset does not exist for code {}", code);
            model.addAttribute("userMessage", "Invalid reset request code. Submit the reset request again.");
            return "reset/requestReset";
        }

        if (resetService.isResetExpired(reset)) {
            log.info("Reset expired for code {}", reset.getCode());
            model.addAttribute("userMessage", "Reset request code expired. Submit the reset request again.");
            return "reset/requestReset";
        }

        if (resetService.isResetComplete(reset)) {
            log.info("Reset is already used for code {}", reset.getCode());
            model.addAttribute("userMessage", "Reset request code is already used. Submit the reset request again.");
            return "reset/requestReset";
        }

        return "";
    }
}
