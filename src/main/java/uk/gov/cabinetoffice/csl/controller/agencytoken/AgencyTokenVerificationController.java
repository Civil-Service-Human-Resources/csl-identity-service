package uk.gov.cabinetoffice.csl.controller.agencytoken;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.VerificationCodeDetermination;
import uk.gov.cabinetoffice.csl.dto.VerificationCodeType;
import uk.gov.cabinetoffice.csl.exception.NotEnoughSpaceAvailableException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.exception.VerificationCodeTypeNotFound;
import uk.gov.cabinetoffice.csl.service.*;
import uk.gov.cabinetoffice.csl.util.Utils;

import static java.lang.String.format;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;

@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/account/verify/agency")
public class AgencyTokenVerificationController {

    private static final String VERIFY_TOKEN_FORM_TEMPLATE = "verifyTokenForm";
    private static final String VERIFY_TOKEN_TEMPLATE = "agencytoken/verifyToken";
    private static final String REDIRECT_VERIFY_TOKEN = "redirect:/account/verify/agency?code=";
    private static final String AGENCY_TOKEN_ASSIGNED_TEMPLATE = "agencytoken/agencyTokenAssigned";
    private static final String REDIRECT_REACTIVATED_SUCCESS = "redirect:/account/reactivate/updated";
    private static final String REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS = "redirect:/account/email/updated";
    private static final String EMAIL_ATTRIBUTE = "email";

    private final EmailUpdateService emailUpdateService;

    private final Utils utils;

    private final AgencyTokenCapacityService agencyTokenCapacityService;

    private final ReactivationService reactivationService;

    private final VerificationCodeDeterminationService verificationCodeDeterminationService;

    private final CsrsService csrsService;

    private final IdentityService identityService;

    private final FrontendService frontendService;

    @GetMapping
    public String enterToken(@RequestParam String code, Model model) {
        log.info("enterToken: User accessing token-based verification screen: code: {}", code);
        if (!model.containsAttribute(VERIFY_TOKEN_FORM_TEMPLATE)) {
            VerifyTokenForm form = new VerifyTokenForm();
            form.setCode(code);
            form.setEmail(verificationCodeDeterminationService.getCodeType(code).getEmail());
            model.addAttribute(VERIFY_TOKEN_FORM_TEMPLATE, form);
        }
        addOrganisationsToModel(model);
        return VERIFY_TOKEN_TEMPLATE;
    }

    @PostMapping
    public String checkToken(Model model,
                             @ModelAttribute @Valid VerifyTokenForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        try {
            log.info("checkToken: Token validation form with values: {}", form);
            if (bindingResult.hasErrors()) {
                buildGenericErrorModel(model, form);
                return VERIFY_TOKEN_TEMPLATE;
            }
            String organisation = form.getOrganisation();
            String token = form.getToken();
            String code = form.getCode();

            VerificationCodeDetermination verificationCodeDetermination =
                    verificationCodeDeterminationService.getCodeType(code);
            String domainFromEmailAddress =
                    utils.getDomainFromEmailAddress(verificationCodeDetermination.getEmail());

            AgencyToken agencyToken = csrsService
                    .getAgencyToken(domainFromEmailAddress, token, organisation)
                    .orElseThrow(() -> new ResourceNotFoundException("Agency Token for DomainTokenOrganisation Not Found"));

            if (!agencyTokenCapacityService.hasSpaceAvailable(agencyToken)) {
                log.warn("Agency token uid = {}, capacity = {}, has no spaces available. Unable to allocate agency token to user {} ",
                        agencyToken.getUid(), agencyToken.getCapacity(), verificationCodeDetermination.getEmail());
                redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, NO_SPACES_AVAILABLE_ERROR_MESSAGE);
                return REDIRECT_VERIFY_TOKEN + code;
            }
            VerificationCodeType verificationCodeType = verificationCodeDetermination.getVerificationCodeType();
            switch (verificationCodeType) {
                case EMAIL_UPDATE -> {
                    log.info("EMAIL_UPDATE agency verification for {}", verificationCodeDetermination);
                    EmailUpdate emailUpdate = emailUpdateService.getEmailUpdateRequestForCode(code);
                    emailUpdateService.updateEmailAddress(emailUpdate, agencyToken);
                    frontendService.signoutUser(emailUpdate.getIdentity().getUid());
                    redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, emailUpdate.getNewEmail());
                    return REDIRECT_ACCOUNT_EMAIL_UPDATED_SUCCESS;
                }
                case REACTIVATION -> {
                    log.info("REACTIVATION agency verification for {}", verificationCodeDetermination);
                    Reactivation reactivation = reactivationService.getReactivationForCodeAndStatus(code, PENDING);
                    reactivationService.reactivateIdentity(reactivation, agencyToken);
                    return REDIRECT_REACTIVATED_SUCCESS;
                }
                case ASSIGN_AGENCY_TOKEN -> {
                    log.info("ASSIGN_AGENCY_TOKEN agency verification for {}", verificationCodeDetermination);
                    identityService.assignAgencyToken(verificationCodeDetermination.getEmail(), agencyToken);
                    return AGENCY_TOKEN_ASSIGNED_TEMPLATE;
                }
                default -> throw new VerificationCodeTypeNotFound(format("Invalid verification code type: %s",
                        verificationCodeType));
            }
        } catch (ResourceNotFoundException e) {
            log.warn("ResourceNotFoundException during agency verification for form: {}", form);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, ENTER_ORG_TOKEN_ERROR_MESSAGE);
            return REDIRECT_VERIFY_TOKEN + form.getCode();
        } catch (NotEnoughSpaceAvailableException e) {
            log.warn("NotEnoughSpaceAvailableException during agency verification for form: {}", form);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, NO_SPACES_AVAILABLE_ERROR_MESSAGE);
            return REDIRECT_VERIFY_TOKEN + form.getCode();
        } catch (Exception e) {
            log.error("Exception during agency verification for form: {}", form);
            redirectAttributes.addFlashAttribute(STATUS_ATTRIBUTE, VERIFY_AGENCY_TOKEN_ERROR_MESSAGE);
            return "redirect:/login";
        }
    }

    private void buildGenericErrorModel(Model model, VerifyTokenForm form) {
        model.addAttribute(STATUS_ATTRIBUTE, ENTER_ORG_TOKEN_ERROR_MESSAGE);
        model.addAttribute(VERIFY_TOKEN_FORM_TEMPLATE, form);
        addOrganisationsToModel(model);
    }

    private void addOrganisationsToModel(Model model) {
        VerifyTokenForm verifyTokenForm = (VerifyTokenForm)model.getAttribute(VERIFY_TOKEN_FORM_TEMPLATE);
        assert verifyTokenForm != null;
        String domain = utils.getDomainFromEmailAddress(verifyTokenForm.getEmail());
        model.addAttribute("organisations", csrsService.getOrganisationalUnitsByDomain(domain));
    }
}
