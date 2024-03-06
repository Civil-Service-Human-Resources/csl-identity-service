package uk.gov.cabinetoffice.csl.controller.signup;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.InviteStatus;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnitDTO;
import uk.gov.cabinetoffice.csl.dto.TokenRequest;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.service.AgencyTokenCapacityService;
import uk.gov.cabinetoffice.csl.service.InviteService;
import uk.gov.cabinetoffice.csl.util.ApplicationConstants;
import uk.gov.cabinetoffice.csl.util.Utils;
import uk.gov.service.notify.NotificationClientException;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/signup")
public class SignupController {

    private static final String ENTER_TOKEN_TEMPLATE = "enterToken";
    private static final String REQUEST_INVITE_TEMPLATE = "requestInvite";
    private static final String INVITE_SENT_TEMPLATE = "inviteSent";
    private static final String SIGNUP_TEMPLATE = "signup";
    private static final String SIGNUP_SUCCESS_TEMPLATE = "signupSuccess";

    private static final String INVITE_MODEL = "invite";
    private static final String ORGANISATIONS_ATTRIBUTE = "organisations";
    private static final String TOKEN_INFO_FLASH_ATTRIBUTE = "tokenRequest";
    private static final String REQUEST_INVITE_FORM = "requestInviteForm";
    private static final String SIGNUP_FORM = "signupForm";
    private static final String ENTER_TOKEN_FORM = "enterTokenForm";

    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_SIGNUP = "redirect:/signup/";
    private static final String REDIRECT_SIGNUP_REQUEST = "redirect:/signup/request";
    private static final String REDIRECT_ENTER_TOKEN = "redirect:/signup/enterToken/";

    private static final String LPG_UI_URL = "lpgUiUrl";

    @Value("${lpg.uiUrl}")
    private String lpgUiUrl;

    @Value("${invite.durationAfterReRegAllowedInSeconds}")
    private long durationAfterReRegAllowedInSeconds;

    private final InviteService inviteService;

    private final IdentityService identityService;

    private final ICivilServantRegistryClient civilServantRegistryClient;

    private final AgencyTokenCapacityService agencyTokenCapacityService;

    private final Utils utils;

    public SignupController(InviteService inviteService,
                            IdentityService identityService,
                            ICivilServantRegistryClient civilServantRegistryClient,
                            AgencyTokenCapacityService agencyTokenCapacityService,
                            Utils utils) {
        this.inviteService = inviteService;
        this.identityService = identityService;
        this.civilServantRegistryClient = civilServantRegistryClient;
        this.agencyTokenCapacityService = agencyTokenCapacityService;
        this.utils = utils;
    }

    @GetMapping(path = "/request")
    public String requestInvite(Model model) {
        model.addAttribute(REQUEST_INVITE_FORM, new RequestInviteForm());
        return REQUEST_INVITE_TEMPLATE;
    }

    @PostMapping(path = "/request")
    public String sendInvite(Model model,
                             @ModelAttribute @Valid RequestInviteForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) throws NotificationClientException {

        if (bindingResult.hasErrors()) {
            model.addAttribute(REQUEST_INVITE_FORM, form);
            return REQUEST_INVITE_TEMPLATE;
        }

        final String email = form.getEmail();
        Optional<Invite> pendingInvite = inviteService.getInviteForEmailAndStatus(email, InviteStatus.PENDING);
        if(pendingInvite.isPresent()) {
            if (inviteService.isInviteExpired(pendingInvite.get())) {
                log.info("{} has already been invited", email);
                inviteService.updateInviteStatus(pendingInvite.get().getCode(), InviteStatus.EXPIRED);
            } else {
                long timeForReReg = new Date().getTime() - pendingInvite.get().getInvitedAt().getTime();
                if (timeForReReg < durationAfterReRegAllowedInSeconds * 1000) {
                    log.info("{} user trying to re-register before re-registration allowed time", email);
                    redirectAttributes.addFlashAttribute(
                            ApplicationConstants.STATUS_ATTRIBUTE,
                            "You have been sent an email with a link to register your account." +
                                    " Please check your spam or junk mail folders.\n" +
                                    "If you have not received the email," +
                                    " please wait %s"
                                            .formatted(utils.convertSecondsIntoMinutesOrHours(durationAfterReRegAllowedInSeconds)) +
                                    " and re-enter your details to create an account.");
                    return REDIRECT_SIGNUP_REQUEST;
                } else {
                    log.info("{} user trying to re-register after re-registration allowed time but " +
                            "before code expired hence setting the current pending invite to expired.", email);
                    inviteService.updateInviteStatus(pendingInvite.get().getCode(), InviteStatus.EXPIRED);
                }
            }
        }

        if (identityService.isIdentityExistsForEmail(email)) {
            log.info("{} is already a user", email);
            redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                    "User already exists with email address " + email);
            return REDIRECT_SIGNUP_REQUEST;
        }

        final String domain = utils.getDomainFromEmailAddress(email);

        if (civilServantRegistryClient.isDomainInAgency(domain)) {
            log.debug("Sending invite to agency user {}", email);
            inviteService.sendSelfSignupInvite(email, false);
            return INVITE_SENT_TEMPLATE;
        } else {
            if (identityService.isAllowListedDomain(domain)) {
                log.debug("Sending invite to allowListed user {}", email);
                inviteService.sendSelfSignupInvite(email, true);
                return INVITE_SENT_TEMPLATE;
            } else {
                log.debug("The domain of user {} is neither allowListed nor part of an Agency token", email);
                redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                        "Your organisation is unable to use this service. Please contact your line manager.");
                return REDIRECT_SIGNUP_REQUEST;
            }
        }
    }

    @GetMapping("/{code}")
    public String signup(Model model, @PathVariable(value = "code") String code,
                                            RedirectAttributes redirectAttributes) {
        if (inviteService.isInviteCodeExists(code)) {
            if (inviteService.isInviteCodeExpired(code)) {
                log.debug("Signup code for invite is expired - redirecting to signup");
                redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                        "This registration link has now expired.\n" +
                                "Please re-enter your details to create an account.");
                inviteService.updateInviteStatus(code, InviteStatus.EXPIRED);
                return REDIRECT_SIGNUP_REQUEST;
            } else {
                Invite invite = inviteService.getInviteForCode(code);

                if (!invite.isAuthorisedInvite()) {
                    log.debug("Invite email = {} not yet authorised - redirecting to enter token screen",
                            invite.getForEmail());
                    return REDIRECT_ENTER_TOKEN + code;
                }

                model.addAttribute(INVITE_MODEL, invite);
                model.addAttribute(SIGNUP_FORM, new SignupForm());

                if (model.containsAttribute(TOKEN_INFO_FLASH_ATTRIBUTE)) {
                    TokenRequest tokenRequest = (TokenRequest) model.asMap().get(TOKEN_INFO_FLASH_ATTRIBUTE);
                    model.addAttribute(TOKEN_INFO_FLASH_ATTRIBUTE, tokenRequest);
                } else {
                    model.addAttribute(TOKEN_INFO_FLASH_ATTRIBUTE, new TokenRequest());
                }
                log.debug("Invite email = {} valid and authorised - redirecting to set password screen",
                        invite.getForEmail());
                return SIGNUP_TEMPLATE;
            }
        } else {
            log.debug("Signup code for invite is not valid - redirecting to signup");
            redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                    "This registration link does not match the one sent to you by email.\n " +
                            "Please check the link and try again.");
            return REDIRECT_SIGNUP_REQUEST;
        }
    }

    @PostMapping("/{code}")
    @Transactional(noRollbackFor = {UnableToAllocateAgencyTokenException.class,
            ResourceNotFoundException.class})
    public String signup(@PathVariable(value = "code") String code,
                         @ModelAttribute @Valid SignupForm signupForm,
                         BindingResult signUpFormBindingResult,
                         @ModelAttribute TokenRequest tokenRequest,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (signUpFormBindingResult.hasErrors()) {
            model.addAttribute(INVITE_MODEL, inviteService.getInviteForCode(code));
            return SIGNUP_TEMPLATE;
        }

        if (inviteService.isInviteValid(code)) {
            Invite invite = inviteService.getInviteForCode(code);
            if (!invite.isAuthorisedInvite()) {
                return REDIRECT_ENTER_TOKEN + code;
            }

            log.debug("Invite and signup credentials valid - creating identity and updating invite to 'Accepted'");
            try {
                identityService.createIdentityFromInviteCode(code, signupForm.getPassword(), tokenRequest);
            } catch (UnableToAllocateAgencyTokenException e) {
                log.debug("UnableToAllocateAgencyTokenException. Redirecting to set password with no spaces error: " + e);

                model.addAttribute(INVITE_MODEL, invite);
                redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                        ApplicationConstants.SIGNUP_NO_SPACES_AVAILABLE_ERROR_MESSAGE);
                redirectAttributes.addFlashAttribute(TOKEN_INFO_FLASH_ATTRIBUTE, tokenRequest);
                return REDIRECT_SIGNUP + code;
            } catch (ResourceNotFoundException e) {
                log.debug("ResourceNotFoundException. Redirecting to set password with error: " + e);

                model.addAttribute(INVITE_MODEL, invite);
                redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                        ApplicationConstants.SIGNUP_RESOURCE_NOT_FOUND_ERROR_MESSAGE);

                return REDIRECT_LOGIN;
            }
            inviteService.updateInviteStatus(code, InviteStatus.ACCEPTED);

            model.addAttribute(LPG_UI_URL, lpgUiUrl);

            return SIGNUP_SUCCESS_TEMPLATE;
        } else {
            return REDIRECT_LOGIN;
        }
    }

    @GetMapping(path = "/enterToken/{code}")
    public String enterToken(Model model, @PathVariable(value = "code") String code) {
        if (inviteService.isInviteValid(code)) {
            Invite invite = inviteService.getInviteForCode(code);
            if (invite.isAuthorisedInvite()) {
                return REDIRECT_SIGNUP + code;
            }

            log.debug("Invite email = {} accessing enter token screen for validation", invite.getForEmail());

            OrganisationalUnitDTO[] organisations = civilServantRegistryClient.getOrganisationalUnitsFormatted();

            model.addAttribute(ORGANISATIONS_ATTRIBUTE, organisations);
            model.addAttribute(ENTER_TOKEN_FORM, new EnterTokenForm());

            return ENTER_TOKEN_TEMPLATE;
        } else {
            return REDIRECT_LOGIN;
        }
    }

    @PostMapping(path = "/enterToken/{code}")
    public String checkToken(Model model,
                             @PathVariable(value = "code") String code,
                             @ModelAttribute @Valid EnterTokenForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ENTER_TOKEN_FORM, form);
            return ENTER_TOKEN_TEMPLATE;
        }

        if (inviteService.isInviteValid(code)) {
            Invite invite = inviteService.getInviteForCode(code);

            final String emailAddress = invite.getForEmail();
            final String domain = utils.getDomainFromEmailAddress(emailAddress);

            return civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(domain, form.getToken(),
                            form.getOrganisation())
                    .map(agencyToken -> {
                        if (!agencyTokenCapacityService.hasSpaceAvailable(agencyToken)) {
                            log.info("Agency token uid = {}, capacity = {}, has no spaces available. " +
                                            "User {} unable to signup",
                                    agencyToken.getUid(), agencyToken.getCapacity(), emailAddress);
                            redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                                    ApplicationConstants.NO_SPACES_AVAILABLE_ERROR_MESSAGE);
                            return REDIRECT_ENTER_TOKEN + code;
                        }

                        invite.setAuthorisedInvite(true);
                        inviteService.saveInvite(invite);

                        model.addAttribute(INVITE_MODEL, invite);

                        redirectAttributes.addFlashAttribute(TOKEN_INFO_FLASH_ATTRIBUTE,
                                addAgencyTokenInfo(domain, form.getToken(), form.getOrganisation()));

                        log.debug("Enter token form has passed domain, token, organisation validation");

                        return REDIRECT_SIGNUP + code;
                    }).orElseGet(() -> {
                        log.debug("Enter token form has failed domain, token, organisation validation");
                        redirectAttributes.addFlashAttribute(ApplicationConstants.STATUS_ATTRIBUTE,
                                ApplicationConstants.ENTER_TOKEN_ERROR_MESSAGE);
                        return REDIRECT_ENTER_TOKEN + code;
                    });
        } else {
            return REDIRECT_LOGIN;
        }
    }

    private TokenRequest addAgencyTokenInfo(String domain, String token, String org) {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setDomain(domain);
        tokenRequest.setToken(token);
        tokenRequest.setOrg(org);
        return tokenRequest;
    }
}
