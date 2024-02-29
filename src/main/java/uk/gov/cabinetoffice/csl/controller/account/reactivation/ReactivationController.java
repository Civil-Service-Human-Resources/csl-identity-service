package uk.gov.cabinetoffice.csl.controller.account.reactivation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.service.AgencyTokenService;
import uk.gov.cabinetoffice.csl.service.NotifyService;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.util.HashMap;
import java.util.Map;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getDecryptedText;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getEncryptedText;

@Slf4j
@Controller
@RequestMapping("/account/reactivate")
public class ReactivationController {

    private static final String ACCOUNT_REACTIVATED_TEMPLATE = "reactivate/accountReactivated";

    private static final String REDIRECT_ACCOUNT_REACTIVATED = "redirect:/account/reactivate/updated";

    private static final String REDIRECT_ACCOUNT_REACTIVATE_AGENCY = "redirect:/account/verify/agency/";

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private static final String LPG_UI_URL_ATTRIBUTE = "lpgUiUrl";

    private final ReactivationService reactivationService;

    private final AgencyTokenService agencyTokenService;

    private final NotifyService notifyService;

    private final Utils utils;

    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;

    @Value("${reactivation.emailTemplateId}")
    private String reactivationEmailTemplateId;

    @Value("${reactivation.reactivationUrl}")
    private String reactivationBaseUrl;

    @Value("${textEncryption.encryptionKey}")
    private String encryptionKey;

    public ReactivationController(ReactivationService reactivationService,
                                  AgencyTokenService agencyTokenService,
                                  NotifyService notifyService,
                                  Utils utils) {
        this.reactivationService = reactivationService;
        this.agencyTokenService = agencyTokenService;
        this.notifyService = notifyService;
        this.utils = utils;
    }

    @SneakyThrows
    @GetMapping
    public String sendReactivationEmail(@RequestParam String code) {
        String email = getDecryptedText(code, encryptionKey);
        if(!reactivationService.isPendingReactivationExistsForEmail(email)) {
            Reactivation reactivation = reactivationService.createPendingReactivation(email);
            notifyUserByEmail(reactivation);
        }
        return "reactivate/reactivate";
    }

    @GetMapping("/{code}")
    public String reactivateAccount(@PathVariable(value = "code") String code, RedirectAttributes redirectAttributes) {
        try {
            Reactivation reactivation = reactivationService.getReactivationForCodeAndStatus(code, PENDING);
            String email = getEncryptedText(reactivation.getEmail(), encryptionKey);
            if(reactivationService.isReactivationExpired(reactivation)){
                log.debug("Reactivation with code {} has expired.", reactivation.getCode());
                return "redirect:/login?error=deactivated-expired&username=" + encode(email, UTF_8);
            }

            String domain = utils.getDomainFromEmailAddress(reactivation.getEmail());
            log.debug("Reactivating account using Reactivation: {}", reactivation);
            if (agencyTokenService.isDomainInAgencyToken(domain)) {
                log.info("Account reactivation is agency, requires token validation for Reactivation: {}",
                        reactivation);
                return REDIRECT_ACCOUNT_REACTIVATE_AGENCY + code;
            } else {
                log.info("Account reactivation is not agency and can reactivate without further validation for Reactivation: {}",
                        reactivation);
                reactivationService.reactivateIdentity(reactivation);
                return REDIRECT_ACCOUNT_REACTIVATED;
            }
        } catch (ResourceNotFoundException e) {
            log.error("ResourceNotFoundException for code: {}, with status {}", code, PENDING);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, REACTIVATION_CODE_IS_NOT_VALID_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        } catch (Exception e) {
            log.error("Unexpected error for code: {} with cause {}",
                    code, e.getCause() != null ? e.getCause().toString() : "Exception cause is null");
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, ACCOUNT_REACTIVATION_ERROR_MESSAGE);
            return REDIRECT_LOGIN;
        }
    }

    @GetMapping("/updated")
    public String accountActivated(Model model) {
        log.info("Account reactivation complete");
        model.addAttribute(LPG_UI_URL_ATTRIBUTE, lpgUiUrl + "/login");
        return ACCOUNT_REACTIVATED_TEMPLATE;
    }

    private void notifyUserByEmail(Reactivation reactivation){
        String email = reactivation.getEmail();

        Map<String, String> emailPersonalisation = new HashMap<>();
        emailPersonalisation.put("learnerName", email);
        emailPersonalisation.put("reactivationUrl", reactivationBaseUrl + reactivation.getCode());

        notifyService.notifyWithPersonalisation(reactivation.getEmail(),
                reactivationEmailTemplateId, emailPersonalisation);
    }
}
