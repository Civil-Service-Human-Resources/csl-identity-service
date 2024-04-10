package uk.gov.cabinetoffice.csl.controller.passwordupdate;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.PasswordService;

@Controller
@RequestMapping("/account/password")
public class UpdatePasswordController {

    private static final String UPDATE_PASSWORD_TEMPLATE = "passwordupdate/updatePassword";
    private static final String PASSWORD_UPDATED_TEMPLATE = "passwordupdate/passwordUpdated";
    private static final String REDIRECT_PASSWORD_UPDATED = "redirect:/account/password/passwordUpdated";

    @Value("${lpg.uiSignOutUrl}")
    private String lpgUiSignOutUrl;

    private final PasswordService passwordService;

    public UpdatePasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @GetMapping
    public String updatePasswordForm(Model model, @ModelAttribute UpdatePasswordForm form) {
        model.addAttribute("updatePasswordForm", form);
        return UPDATE_PASSWORD_TEMPLATE;
    }

    @PostMapping
    public String updatePassword(Model model, @Valid @ModelAttribute UpdatePasswordForm form,
                                 BindingResult bindingResult, Authentication authentication) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("updatePasswordForm", form);
            return UPDATE_PASSWORD_TEMPLATE;
        }

        passwordService.updatePasswordAndNotify(
                ((IdentityDetails) authentication.getPrincipal()).getIdentity(),
                form.getNewPassword());
        return REDIRECT_PASSWORD_UPDATED;
    }

    @GetMapping("/passwordUpdated")
    public String passwordUpdated(Model model) {
        model.addAttribute("lpgUiSignOutUrl", lpgUiSignOutUrl);
        return PASSWORD_UPDATED_TEMPLATE;
    }
}
