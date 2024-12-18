package uk.gov.cabinetoffice.csl.controller.emailupdate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import uk.gov.cabinetoffice.csl.service.EmailUpdateService;
import uk.gov.cabinetoffice.csl.service.FrontendService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.util.LogoutUtil;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.util.Map;

import static uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus.UPDATED;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;

@Slf4j
@Controller
@RequestMapping("/account/email")
public class EmailUpdateController {
    private static final String LPG_UI_URL_ATTRIBUTE = "lpgUiUrl";
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String UPDATED_EMAIL_ATTRIBUTE = "updatedEmail";

    private static final String UPDATE_EMAIL_FORM = "updateEmailForm";
    private static final String UPDATE_EMAIL_TEMPLATE = "emailupdate/updateEmail";
    private static final String UPDATE_EMAIL_ERROR_TEMPLATE = "emailupdate/updateEmailError";
    private static final String EMAIL_UPDATED_TEMPLATE = "emailupdate/emailUpdated";
    private static final String EMAIL_VERIFICATION_SENT_TEMPLATE = "emailupdate/emailVerificationSent";

    private static final String REDIRECT_ACCOUNT_EMAIL_INVALID_EMAIL_TRUE = "redirect:/account/email/update/error?invalidEmail=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_ALREADY_TAKEN_TRUE = "redirect:/account/email/update/error?emailAlreadyTaken=true";
    private static final String REDIRECT_UPDATE_EMAIL_NOT_VALID_EMAIL_DOMAIN_TRUE = "redirect:/account/email/update/error?notValidEmailDomain=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_INVALID_CODE_TRUE = "redirect:/account/email/update/error?invalidCode=true";
    private static final String REDIRECT_ACCOUNT_EMAIL_CODE_EXPIRED_TRUE = "redirect:/account/email/update/error?codeExpired=true";

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private static final String REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS = "redirect:/account/email/updated";
    private static final String REDIRECT_ACCOUNT_ENTER_TOKEN = "redirect:/account/verify/agency/";

    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;

    @Value("${lpg.contactEmail}")
    private String contactEmail;

    @Value("${lpg.contactNumber}")
    private String contactNumber;

    private final IdentityService identityService;
    private final FrontendService frontendService;
    private final EmailUpdateService emailUpdateService;
    private final Utils utils;
    private final LogoutUtil logoutUtil;
    private final int validityInSeconds;

    public EmailUpdateController(IdentityService identityService,
                                 FrontendService frontendService, EmailUpdateService emailUpdateService,
                                 Utils utils, LogoutUtil logoutUtil,
                                 @Value("${emailUpdate.validityInSeconds}") int validityInSeconds) {
        this.identityService = identityService;
        this.frontendService = frontendService;
        this.emailUpdateService = emailUpdateService;
        this.utils = utils;
        this.logoutUtil = logoutUtil;
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

        model.addAttribute(CONTACT_EMAIL_ATTRIBUTE, contactEmail);
        model.addAttribute(CONTACT_NUMBER_ATTRIBUTE, contactNumber);

        if (bindingResult.hasErrors()) {
            model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
            model.addAttribute(UPDATE_EMAIL_FORM, form);
            return UPDATE_EMAIL_TEMPLATE;
        }
        String newEmail = form.getEmail();
        log.info("Email update requested, sending change email link to {} for verification", newEmail);

        if (identityService.isIdentityExistsForEmail(newEmail)) {
            log.warn("Email {} is already in use", newEmail);
            return REDIRECT_ACCOUNT_EMAIL_ALREADY_TAKEN_TRUE;
        }

        if (!identityService.isValidEmailDomain(newEmail)) {
            log.warn("Email {} is neither allow listed nor an agency token", newEmail);
            return REDIRECT_UPDATE_EMAIL_NOT_VALID_EMAIL_DOMAIN_TRUE;
        }

        Identity identity = ((IdentityDetails) authentication.getPrincipal()).getIdentity();
        emailUpdateService.saveEmailUpdateAndNotify(identity, newEmail);
        model.addAttribute("resetValidity", utils.convertSecondsIntoDaysHoursMinutesSeconds(validityInSeconds));
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
        return EMAIL_VERIFICATION_SENT_TEMPLATE;
    }

    @GetMapping("/verify/{code}")
    public String verifyEmail(@PathVariable String code, Authentication authentication,
                              HttpServletRequest request, HttpServletResponse response,
                              RedirectAttributes redirectAttributes) {
        log.debug("Attempting update email verification with code: {}", code);

        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if(principal != null ) {
            logoutUtil.logout(request, response);
            log.debug("verifyEmail: logoutUtil.logout is invoked.");
        }

        if (!emailUpdateService.isEmailUpdateRequestExistsForCode(code)) {
            log.warn("Email update code {} does not exist", code);
            return REDIRECT_ACCOUNT_EMAIL_INVALID_CODE_TRUE;
        }

        EmailUpdate emailUpdate = emailUpdateService.getEmailUpdateRequestForCode(code);
        String oldEmail = emailUpdate.getPreviousEmail();
        String newEmail = emailUpdate.getNewEmail();

        if(UPDATED.equals(emailUpdate.getEmailUpdateStatus())) {
            log.info("Email update code {} is already used. oldEmail = {}, newEmail = {}", code, oldEmail, newEmail);
            redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, newEmail);
            return REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS;
        }

        if(!identityService.isIdentityExistsForEmail(oldEmail)) {
            log.info("Unable to update email for the code {}. identity not found for email {}", code, oldEmail);
            return REDIRECT_ACCOUNT_EMAIL_INVALID_EMAIL_TRUE;
        }

        if(emailUpdateService.isEmailUpdateExpired(emailUpdate)) {
            log.info("Email update code {} expired. oldEmail = {}, newEmail = {}", code, oldEmail, newEmail);
            return REDIRECT_ACCOUNT_EMAIL_CODE_EXPIRED_TRUE;
        }

        String newDomain = utils.getDomainFromEmailAddress(newEmail);
        log.debug("Attempting update email verification with domain: {}", newDomain);

        if (identityService.isDomainInAnAgencyToken(newDomain)) {
            log.debug("New email domain is in agency. oldEmail = {}, newEmail = {}", oldEmail,
                    newEmail);
            redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, newEmail);
            return REDIRECT_ACCOUNT_ENTER_TOKEN + code;
        } else if (identityService.isDomainAllowListed(newDomain)) {
            log.debug("New email domain is allow listed. oldEmail = {}, newEmail = {}", oldEmail,
                    newEmail);
            try {
                emailUpdateService.updateEmailAddress(emailUpdate);
                log.debug("Email updated successfully from old email = {} to newEmail = {}", oldEmail, newEmail);
                frontendService.signoutUser(emailUpdate.getIdentity().getUid());
                redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, newEmail);
                return REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS;
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, CHANGE_EMAIL_ERROR_MESSAGE);
                log.error("Unable to update old email = {} to newEmail = {}. Exception: {}", oldEmail, newEmail
                        , e.toString());
                return REDIRECT_LOGIN;
            }
        } else {
            log.warn("User trying to verify change email where new email is neither allow listed nor an agency token: " +
                    "oldEmail = {}, newEmail = {}", oldEmail, newEmail);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, CHANGE_EMAIL_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        }
    }

    @GetMapping("/updated")
    public String emailUpdated(Model model) {
        Map<String, Object> modelMap = model.asMap();
        String updatedEmail = String.valueOf(modelMap.get(EMAIL_ATTRIBUTE));
        model.addAttribute(UPDATED_EMAIL_ATTRIBUTE, updatedEmail);
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
        return EMAIL_UPDATED_TEMPLATE;
    }

    @GetMapping("/update/error")
    public String emailUpdateError(Model model) {
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl);
        return UPDATE_EMAIL_ERROR_TEMPLATE;
    }
}
