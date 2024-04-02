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
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.EmailUpdateService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.util.Map;

import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CHANGE_EMAIL_ERROR_MESSAGE;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.STATUS_ATTRIBUTE;

@Slf4j
@Controller
@RequestMapping("/account/email")
public class EmailUpdateController {
    private static final String LPG_UI_SIGNOUT_URL_ATTRIBUTE = "lpgUiSignOutUrl";
    private static final String LPG_UI_URL_ATTRIBUTE = "lpgUiUrl";
    private static final String EMAIL_ATTRIBUTE = "email";

    private static final String UPDATE_EMAIL_FORM = "updateEmailForm";
    private static final String UPDATE_EMAIL_TEMPLATE = "account/updateEmail";
    private static final String EMAIL_UPDATED_TEMPLATE = "account/emailUpdated";
    private static final String EMAIL_VERIFICATION_SENT_TEMPLATE = "account/emailVerificationSent";

    private static final String REDIRECT_ACCOUNT_EMAIL_INVALID_EMAIL_TRUE = "redirect:/account/email?invalidEmail=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_ALREADY_TAKEN_TRUE = "redirect:/account/email?emailAlreadyTaken=true";
    private static final String REDIRECT_UPDATE_EMAIL_NOT_VALID_EMAIL_DOMAIN_TRUE = "redirect:/account/email?notValidEmailDomain=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_INVALID_CODE_TRUE = "redirect:/account/email?invalidCode=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_CODE_EXPIRED_TRUE = "redirect:/account/email?codeExpired=true";

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private static final String REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS = "redirect:/account/email/updated";
    private static final String REDIRECT_ACCOUNT_ENTER_TOKEN = "redirect:/account/verify/agency/";

    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;

    @Value("${lpg.uiSignOutUrl}")
    private String lpgUiSignOutUrl;

    private final IdentityService identityService;
    private final EmailUpdateService emailUpdateService;
    private final Utils utils;
    private final int validityInSeconds;

    public EmailUpdateController(IdentityService identityService,
                                 EmailUpdateService emailUpdateService,
                                 Utils utils,
                                 @Value("${emailUpdate.validityInSeconds}") int validityInSeconds) {
        this.identityService = identityService;
        this.emailUpdateService = emailUpdateService;
        this.utils = utils;
        this.validityInSeconds = validityInSeconds;
    }

    @GetMapping
    public String updateEmailForm(Model model, @ModelAttribute UpdateEmailForm form) {
        log.debug("Getting update email form");
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
        model.addAttribute(UPDATE_EMAIL_FORM, form);
        return UPDATE_EMAIL_TEMPLATE;
    }

    @PostMapping
    public String sendEmailVerification(Model model, @Valid @ModelAttribute UpdateEmailForm form,
                                        BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return UPDATE_EMAIL_TEMPLATE;
        }
        String newEmail = form.getEmail();
        log.info("Change email requested, sending email to {} for verification", newEmail);

        if (identityService.isIdentityExistsForEmail(newEmail)) {
            log.warn("Email already in use: {}", newEmail);
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return REDIRECT_ACCOUNT_EMAIL_ALREADY_TAKEN_TRUE;
        }

        if (!identityService.isValidEmailDomain(newEmail)) {
            log.warn("Email is neither allow listed or for an agency token: {}", newEmail);
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return REDIRECT_UPDATE_EMAIL_NOT_VALID_EMAIL_DOMAIN_TRUE;
        }

        emailUpdateService.saveEmailUpdateAndNotify(((IdentityDetails) authentication.getPrincipal()).getIdentity(),
                newEmail);

        model.addAttribute("resetValidity", utils.convertSecondsIntoMinutesOrHours(validityInSeconds));
        model.addAttribute(LPG_UI_SIGNOUT_URL_ATTRIBUTE, lpgUiSignOutUrl);
        return EMAIL_VERIFICATION_SENT_TEMPLATE;
    }

    @GetMapping("/verify/{code}")
    public String verifyEmail(@PathVariable String code,
                              RedirectAttributes redirectAttributes) {
        log.debug("Attempting update email verification with code: {}", code);

        if (!emailUpdateService.isEmailUpdateRequestExistsForCode(code)) {
            log.warn("Email update code does not exist: {}", code);
            return REDIRECT_ACCOUNT_EMAIL_INVALID_CODE_TRUE;
        }

        EmailUpdate emailUpdate = emailUpdateService.getEmailUpdateRequestForCode(code);
        String oldEmail = emailUpdate.getPreviousEmail();
        String newEmail = emailUpdate.getNewEmail();

        if(!identityService.isIdentityExistsForEmail(oldEmail)) {
            log.info("Unable to update email for the code {}. identity not found for email {}", code, oldEmail);
            return REDIRECT_ACCOUNT_EMAIL_INVALID_EMAIL_TRUE;
        }

        if(emailUpdateService.isEmailUpdateExpired(emailUpdate)) {
            log.info("Email update code expired: {} oldEmail {}, newEmail: {}", code, oldEmail, newEmail);
            return REDIRECT_ACCOUNT_EMAIL_CODE_EXPIRED_TRUE;
        }

        String newDomain = utils.getDomainFromEmailAddress(newEmail);
        log.debug("Attempting update email verification with domain: {}", newDomain);

        if (isAgencyDomain(newDomain)) {
            log.debug("New email is agency: oldEmail = {}, newEmail = {}", oldEmail,
                    newEmail);
            redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, newEmail);
            return REDIRECT_ACCOUNT_ENTER_TOKEN + code;
        } else if (isAllowListed(newDomain)) {
            log.debug("New email is allow listed: oldEmail = {}, newEmail = {}", oldEmail,
                    newEmail);
            try {
                emailUpdateService.updateEmailAddress(emailUpdate);
                redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, newEmail);
                return REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS;
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, CHANGE_EMAIL_ERROR_MESSAGE);
                log.error("Unable to update email oldEmail = {}, newEmail = {}. Exception: {}", oldEmail, newEmail
                        , e.toString());
                return REDIRECT_LOGIN;
            }
        } else {
            log.warn("User trying to verify change email where new email is not allow listed or agency: " +
                    "oldEmail = {}, newEmail = {}", oldEmail, newEmail);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, CHANGE_EMAIL_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        }
    }

    @GetMapping("/updated")
    public String emailUpdated(Model model) {
        Map<String, Object> modelMap = model.asMap();
        String updatedEmail = String.valueOf(modelMap.get(EMAIL_ATTRIBUTE));

        model.addAttribute("updatedEmail", updatedEmail);
        model.addAttribute(LPG_UI_SIGNOUT_URL_ATTRIBUTE, lpgUiSignOutUrl);

        log.debug("Email updated success for: {}", updatedEmail);
        return EMAIL_UPDATED_TEMPLATE;
    }

    private boolean isAllowListed(String newDomain) {
        return identityService.isAllowListedDomain(newDomain);
    }

    private boolean isAgencyDomain(String newDomain) {
        return identityService.isDomainInAgency(newDomain);
    }
}
