package uk.gov.cabinetoffice.csl.controller.reset;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
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
    public String requestReset(@RequestParam(value = "email") String email) throws Exception {
        log.info("Requesting reset for {} ", email);

        if (identityRepository.existsByEmail(email)) {
            resetService.notifyForResetRequest(email);
        }

        return "reset/checkEmail";
    }

    @GetMapping("/{code}")
    public String loadResetForm(@PathVariable(value = "code") String code, Model model) {
        log.debug("User on reset screen with code {}", code);

        checkResetCodeExists(code);

        Reset reset = resetRepository.findByCode(code);

        if (isResetInvalid(reset)) {
            return "redirect:/reset";
        }

        ResetForm resetForm = new ResetForm();
        resetForm.setCode(code);

        model.addAttribute("resetForm", resetForm);

        return "reset/passwordForm";
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

        checkResetCodeExists(code);

        Reset reset = resetRepository.findByCode(code);

        if (isResetInvalid(reset)) {
            return "redirect:/reset";
        }

        if (reset == null || reset.getEmail() == null) {
            log.info("Reset does not exist with code {}", code);
            throw new ResourceNotFoundException();
        }

        Identity identity = identityRepository.findFirstByEmailEquals(reset.getEmail());

        if (identity == null || identity.getEmail() == null) {
            log.info("Identity does not exist with reset code {}", code);
            throw new ResourceNotFoundException();
        }

        userService.updatePassword(identity, resetForm.getPassword());

        resetService.notifyOfSuccessfulReset(reset);

        log.info("Password reset successfully for {}", identity.getEmail());

        model.addAttribute("lpgUiUrl", lpgUiUrl);

        return "reset/passwordReset";
    }

    @InitBinder
    public void resetValidation(WebDataBinder binder) {
        if (binder.getTarget() instanceof ResetForm) {
            binder.addValidators(resetFormValidator);
        }
    }

    private boolean isResetInvalid(Reset reset) {
        if (resetService.isResetExpired(reset) || !resetService.isResetPending(reset)) {
            log.info("Reset is not valid for code {}", reset.getCode());
            return true;
        }
        return false;
    }

    private void checkResetCodeExists(String code) {
        if (!resetRepository.existsByCode(code)) {
            log.info("Reset code does not exist {}", code);
            throw new ResourceNotFoundException();
        }
    }
}
