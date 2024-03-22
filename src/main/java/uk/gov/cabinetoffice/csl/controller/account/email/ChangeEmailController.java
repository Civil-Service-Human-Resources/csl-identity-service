package uk.gov.cabinetoffice.csl.controller.account.email;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.service.AgencyTokenService;
import uk.gov.cabinetoffice.csl.service.EmailUpdateService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.util.Map;

import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CHANGE_EMAIL_ERROR_MESSAGE;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.STATUS_ATTRIBUTE;

@Slf4j
@Controller
@RequestMapping("/account/email")
public class ChangeEmailController {
    private static final String LPG_UI_URL_ATTRIBUTE = "lpgUiUrl";
    private static final String EMAIL_ATTRIBUTE = "email";

    private static final String UPDATE_EMAIL_FORM = "updateEmailForm";
    private static final String UPDATE_EMAIL_TEMPLATE = "account/updateEmail";
    private static final String EMAIL_UPDATED_TEMPLATE = "account/emailUpdated";
    private static final String EMAIL_VERIFICATION_SENT_TEMPLATE = "account/emailVerificationSent";

    private static final String REDIRECT_ACCOUNT_EMAIL_ALREADY_TAKEN_TRUE = "redirect:/account/email?emailAlreadyTaken=true";
    private static final String REDIRECT_UPDATE_EMAIL_NOT_VALID_EMAIL_DOMAIN_TRUE = "redirect:/account/email?notValidEmailDomain=true";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_ACCOUNT_EMAIL_INVALID_EMAIL_TRUE = "redirect:/account/email?invalidEmail=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS = "redirect:/account/email/updated";
    private static final String REDIRECT_ACCOUNT_ENTER_TOKEN = "redirect:/account/verify/agency/";

    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;

    private final IdentityService identityService;
    private final EmailUpdateService emailUpdateService;
    private final AgencyTokenService agencyTokenService;
    private final Utils utils;

    public ChangeEmailController(IdentityService identityService,
                                 EmailUpdateService emailUpdateService,
                                 AgencyTokenService agencyTokenService,
                                 Utils utils) {
        this.identityService = identityService;
        this.emailUpdateService = emailUpdateService;
        this.agencyTokenService = agencyTokenService;
        this.utils = utils;
    }

    @GetMapping
    public String updateEmailForm(Model model, @ModelAttribute UpdateEmailForm form) {
        log.debug("Getting update email form");
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
        model.addAttribute(UPDATE_EMAIL_FORM, form);
        return UPDATE_EMAIL_TEMPLATE;
    }

    @PostMapping
    public String sendEmailVerification(Model model, @Valid @ModelAttribute UpdateEmailForm form, BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return UPDATE_EMAIL_TEMPLATE;
        }
        String newEmail = form.getEmail();
        log.info("Change email requested, sending email to {} for verification", newEmail);

        if (identityService.isIdentityExistsForEmail(newEmail)) {
            log.error("Email already in use: {}", newEmail);
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return REDIRECT_ACCOUNT_EMAIL_ALREADY_TAKEN_TRUE;
        }

        if (!identityService.isValidEmailDomain(newEmail)) {
            log.error("Email is neither allowlisted or for an agency token: {}", newEmail);
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return REDIRECT_UPDATE_EMAIL_NOT_VALID_EMAIL_DOMAIN_TRUE;
        }

        emailUpdateService.saveEmailUpdateAndNotify(((IdentityDetails) authentication.getPrincipal()).getIdentity(),
                newEmail);

        return EMAIL_VERIFICATION_SENT_TEMPLATE;
    }

    @GetMapping("/verify/{code}")
    public String verifyEmail(@PathVariable String code,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        log.debug("Attempting update email verification with code: {}", code);

        Identity identity = ((IdentityDetails) authentication.getPrincipal()).getIdentity();

        if (!emailUpdateService.existsByCode(code)) {
            log.error("Unable to verify email update code: {}", code);
            return "redirect:/account/email?invalidCode=true";
        }

        EmailUpdate emailUpdate = emailUpdateService.getEmailUpdateByCode(code);
        String newDomain = utils.getDomainFromEmailAddress(emailUpdate.getEmail());

        log.debug("Attempting update email verification with domain: {}", newDomain);

        if (isAgencyDomain(newDomain)) {
            log.debug("New email is agency: oldEmail = {}, newEmail = {}", identity.getEmail(),
                    emailUpdate.getEmail());
            redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, emailUpdate.getEmail());
            return REDIRECT_ACCOUNT_ENTER_TOKEN + code;
        } else if (isAllowListed(newDomain)) {
            log.debug("New email is allowlisted: oldEmail = {}, newEmail = {}", identity.getEmail(),
                    emailUpdate.getEmail());
            try {
                emailUpdateService.updateEmailAddress(emailUpdate);
                redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, emailUpdate.getEmail());
                return REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS;
            } catch (ResourceNotFoundException e) {
                log.error("Unable to update email, redirecting to enter token screen: {} {}", code, identity);
                return REDIRECT_ACCOUNT_EMAIL_INVALID_EMAIL_TRUE;
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, CHANGE_EMAIL_ERROR_MESSAGE);
                log.error("Unable to update email: {} {}. {}", code, identity, e.toString());
                return REDIRECT_LOGIN;
            }
        } else {
            log.error("User trying to verify change email where new email is not allowlisted or agency: " +
                    "oldEmail = {}, newEmail = {}", identity.getEmail(), emailUpdate.getEmail());
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, CHANGE_EMAIL_ERROR_MESSAGE);

            return REDIRECT_LOGIN;
        }
    }

    @GetMapping("/updated")
    public String emailUpdated(Model model) {
        Map<String, Object> modelMap = model.asMap();
        String updatedEmail = String.valueOf(modelMap.get(EMAIL_ATTRIBUTE));

        model.addAttribute("updatedEmail", updatedEmail);
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl + "/sign-out");

        log.debug("Email updated success for: {}", updatedEmail);
        return EMAIL_UPDATED_TEMPLATE;
    }

    private boolean isAllowListed(String newDomain) {
        return identityService.isAllowListedDomain(newDomain);
    }

    private boolean isAgencyDomain(String newDomain) {
        return agencyTokenService.isDomainInAgencyToken(newDomain);
    }
}
