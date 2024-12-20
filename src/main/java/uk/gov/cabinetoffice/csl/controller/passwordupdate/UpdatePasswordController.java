package uk.gov.cabinetoffice.csl.controller.passwordupdate;

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
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.FrontendService;
import uk.gov.cabinetoffice.csl.service.PasswordService;

@Slf4j
@Controller
@RequestMapping("/account/password")
public class UpdatePasswordController {

    private static final String UPDATE_PASSWORD_TEMPLATE = "passwordupdate/updatePassword";
    private static final String PASSWORD_UPDATED_TEMPLATE = "passwordupdate/passwordUpdated";
    private static final String LPG_UI_URL_ATTRIBUTE = "lpgUiUrl";
    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;
    private final PasswordService passwordService;
    private final FrontendService frontendService;

    public UpdatePasswordController(PasswordService passwordService, FrontendService frontendService) {
        this.passwordService = passwordService;
        this.frontendService = frontendService;
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

        Identity identity = ((IdentityDetails) authentication.getPrincipal()).getIdentity();
        passwordService.updatePasswordAndNotify(identity, form.getNewPassword());
        log.info("Password updated for {}", identity.getEmail());
        frontendService.signoutUser(identity.getUid());
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
        return PASSWORD_UPDATED_TEMPLATE;
    }
}
