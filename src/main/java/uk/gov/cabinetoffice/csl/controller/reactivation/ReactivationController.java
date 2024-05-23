package uk.gov.cabinetoffice.csl.controller.reactivation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.NotifyService;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.EXPIRED;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getDecryptedText;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getEncryptedText;

@Slf4j
@Controller
@RequestMapping("/account/reactivate")
public class ReactivationController {

    private static final String ACCOUNT_REACTIVATED_TEMPLATE = "reactivate/accountReactivated";

    private static final String ACCOUNT_REACTIVATE_TEMPLATE = "reactivate/reactivate";

    private static final String REDIRECT_ACCOUNT_REACTIVATED = "redirect:/account/reactivate/updated";

    private static final String REDIRECT_ACCOUNT_REACTIVATE_AGENCY = "redirect:/account/verify/agency/";

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private static final String LPG_UI_URL_ATTRIBUTE = "lpgUiUrl";

    private final ReactivationService reactivationService;

    private final IdentityService identityService;

    private final NotifyService notifyService;

    private final Utils utils;

    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;

    @Value("${govNotify.template.reactivation}")
    private String govNotifyReactivationTemplateId;

    @Value("${reactivation.reactivationUrl}")
    private String reactivationBaseUrl;

    @Value("${textEncryption.encryptionKey}")
    private String encryptionKey;

    @Value("${reactivation.validityInSeconds}")
    private int reactivationValidityInSeconds;

    public ReactivationController(ReactivationService reactivationService,
                                  IdentityService identityService,
                                  NotifyService notifyService,
                                  Utils utils) {
        this.reactivationService = reactivationService;
        this.identityService = identityService;
        this.notifyService = notifyService;
        this.utils = utils;
    }

    @GetMapping
    public String sendReactivationEmail(@RequestParam String code, Model model, RedirectAttributes redirectAttributes) {
        try {
            String email = getDecryptedText(code, encryptionKey);

            String resultIdentityActive = checkIdentityActive(email, redirectAttributes);
            if(isNotBlank(resultIdentityActive)) {
                return resultIdentityActive;
            }

            if(reactivationService.isPendingReactivationExistsForEmail(email)) {
                Reactivation pendingReactivation = reactivationService.getPendingReactivationForEmail(email);
                LocalDateTime requestedAt = pendingReactivation.getRequestedAt();
                String reactivationEmailMessage = ("We've sent you an email on %s with a link to reactivate your " +
                        "account.").formatted(utils.convertDateTimeFormat(requestedAt));
                model.addAttribute("reactivationEmailMessage", reactivationEmailMessage);
                LocalDateTime reactivationLinkExpiryDateTime = requestedAt.plusSeconds(reactivationValidityInSeconds);
                String reactivationValidityMessage = "The link in the email will expire on %s."
                        .formatted(utils.convertDateTimeFormat(reactivationLinkExpiryDateTime));

                model.addAttribute("reactivationValidityMessage", reactivationValidityMessage);
            } else {
                Reactivation reactivation = reactivationService.createPendingReactivation(email);
                notifyUserByEmail(reactivation);
                String reactivationEmailMessage = "We've sent you an email with a link to reactivate your account.";
                model.addAttribute("reactivationEmailMessage", reactivationEmailMessage);
                String reactivationValidityMessage = "You have %s to click the reactivation link within the email."
                        .formatted(utils.convertSecondsIntoDaysHoursMinutesSeconds(reactivationValidityInSeconds));
                model.addAttribute("reactivationValidityMessage", reactivationValidityMessage);
            }
            return ACCOUNT_REACTIVATE_TEMPLATE;
        } catch (Exception e) {
            log.error("There was an error while creating the reactivation link for the code: {} with cause: {}",
                    code, e.toString());
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, ACCOUNT_REACTIVATION_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        }
    }

    @GetMapping("/{code}")
    public String reactivateAccount(@PathVariable(value = "code") String code, RedirectAttributes redirectAttributes) {
        try {
            Reactivation reactivation = reactivationService.getReactivationForCodeAndStatus(code, PENDING);
            String email = reactivation.getEmail();

            String resultIdentityActive = checkIdentityActive(email, redirectAttributes);
            if(isNotBlank(resultIdentityActive)) {
                return resultIdentityActive;
            }

            if(reactivationService.isReactivationExpired(reactivation)) {
                log.debug("Reactivation with code {} has expired.", reactivation.getCode());
                return "redirect:/login?error=reactivation-expired&username="
                        + encode(getEncryptedText(email, encryptionKey), UTF_8);
            }

            String domain = utils.getDomainFromEmailAddress(reactivation.getEmail());
            log.debug("Reactivating account using Reactivation: {}", reactivation);
            if (identityService.isDomainInAnAgencyToken(domain)) {
                log.info("Account reactivation is for a agency domain, requires token validation for Reactivation: {}",
                        reactivation);
                return REDIRECT_ACCOUNT_REACTIVATE_AGENCY + code;
            } else {
                log.info("Account reactivation is not a agency domain and can reactivate without further validation for " +
                                "Reactivation: {}", reactivation);
                reactivationService.reactivateIdentity(reactivation);
                return REDIRECT_ACCOUNT_REACTIVATED;
            }
        } catch (ResourceNotFoundException e) {
            log.warn("ResourceNotFoundException for code: {}, with status: {}", code, PENDING);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, REACTIVATION_CODE_IS_NOT_VALID_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        } catch (Exception e) {
            log.error("There was an error processing account reactivation for code: {} with cause: {}",
                    code, e.getCause() != null ? e.getCause().toString() : "Exception cause was null");
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, ACCOUNT_REACTIVATION_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        }
    }

    @GetMapping("/updated")
    public String accountActivated(Model model) {
        log.info("Account reactivation complete.");
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl + "/login");
        return ACCOUNT_REACTIVATED_TEMPLATE;
    }

    private String checkIdentityActive(String email, RedirectAttributes redirectAttributes) {
        Identity identityForEmail = identityService.getIdentityForEmail(email);

        if(identityForEmail == null) {
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, REACTIVATION_CODE_IS_NOT_VALID_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        }

        if(identityForEmail.isActive()) {
            try {
                Reactivation pendingReactivation = reactivationService.getPendingReactivationForEmail(email);
                pendingReactivation.setReactivationStatus(EXPIRED);
                reactivationService.saveReactivation(pendingReactivation);
                log.info("Pending reactivations are marked as expired because user is active for email: {}",
                        email);
            } catch(Exception e) {
                log.warn("Pending reactivation not found for email: {}", email);
            }
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, REACTIVATION_ACCOUNT_IS_ALREADY_ACTIVE);
            return REDIRECT_LOGIN;
        }
        return null;
    }

    private void notifyUserByEmail(Reactivation reactivation){
        String email = reactivation.getEmail();

        Map<String, String> emailPersonalisation = new HashMap<>();
        emailPersonalisation.put("learnerName", email);
        emailPersonalisation.put("reactivationUrl", reactivationBaseUrl + reactivation.getCode());

        notifyService.notifyWithPersonalisation(reactivation.getEmail(),
                govNotifyReactivationTemplateId, emailPersonalisation);
    }
}
