package uk.gov.cabinetoffice.csl.controller.account.password;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.cabinetoffice.csl.service.UserService;

@Slf4j
@Controller
@RequestMapping("/account/password")
public class UpdatePasswordController {

    private final UserService userService;
    private final String lpgUiUrl;

    public UpdatePasswordController(UserService userService,
                                    @Value("${lpg.uiUrl}") String lpgUiUrl) {
        this.userService = userService;
        this.lpgUiUrl = lpgUiUrl;
    }

    @GetMapping
    public String updatePasswordForm(Model model, @ModelAttribute UpdatePasswordForm form) {
        model.addAttribute("updatePasswordForm", form);
        return "account/updatePassword";
    }

    @PostMapping
    public String updatePassword(Model model, @Valid @ModelAttribute UpdatePasswordForm form,
                                 BindingResult bindingResult, Authentication authentication) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("updatePasswordForm", form);
            return "account/updatePassword";
        }

        userService.updatePasswordAndNotify(
                ((IdentityDetails) authentication.getPrincipal()).getIdentity(),
                form.getNewPassword());
        return "redirect:/account/password/passwordUpdated";
    }

    @GetMapping("/passwordUpdated")
    public String passwordUpdated(Model model) {
        model.addAttribute("lpgUiUrl", lpgUiUrl);
        return "account/passwordUpdated";
    }
}
