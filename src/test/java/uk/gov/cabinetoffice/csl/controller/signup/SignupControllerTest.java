package uk.gov.cabinetoffice.csl.controller.signup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.Domain;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;
import uk.gov.cabinetoffice.csl.service.CsrsService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.service.AgencyTokenCapacityService;
import uk.gov.cabinetoffice.csl.service.InviteService;

import java.time.Clock;
import java.util.*;

import static java.time.LocalDateTime.now;
import static org.hamcrest.Matchers.containsString;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.domain.InviteStatus.EXPIRED;
import static uk.gov.cabinetoffice.csl.domain.InviteStatus.PENDING;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class SignupControllerTest {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";
    private static final String STATUS_ATTRIBUTE = "status";

    private static final String ENTER_TOKEN_TEMPLATE = "agencytoken/enterToken";
    private static final String CHOOSE_ORGANISATION_TEMPLATE = "signup/chooseOrganisation";
    private static final String REQUEST_INVITE_TEMPLATE = "signup/requestInvite";
    private static final String INVITE_SENT_TEMPLATE = "signup/inviteSent";
    private static final String SIGNUP_TEMPLATE = "signup/signup";
    private static final String SIGNUP_SUCCESS_TEMPLATE = "signup/signupSuccess";
    private static final String PENDING_SIGNUP_TEMPLATE = "signup/pendingSignup";

    private static final String REDIRECT_SIGNUP = "/signup/";
    private static final String REDIRECT_SIGNUP_REQUEST = "/signup/request";
    private static final String REDIRECT_CHOOSE_ORGANISATION = "/signup/chooseOrganisation/";
    private static final String REDIRECT_ENTER_TOKEN = "/signup/enterToken/";
    private static final String REDIRECT_INVALID_SIGNUP_CODE = "/login?error=invalidSignupCode";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InviteService inviteService;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @MockBean
    private CsrsService csrsService;

    private final Clock clock = Clock.systemUTC();

    private final String GENERIC_EMAIL = "email@domain.com";
    private final String GENERIC_DOMAIN = "domain.com";
    private final String GENERIC_CODE = "ABC123";
    private final String GENERIC_ORG_CODE = "org123";


    private Invite generateBasicInvite(boolean authorised) {
        Invite i = new Invite();
        i.setCode(GENERIC_CODE);
        i.setForEmail(GENERIC_EMAIL);
        i.setAuthorisedInvite(authorised);
        return i;
    }

    private OrganisationalUnit generateBasicOrganisation() {
        OrganisationalUnit organisationalUnit = new OrganisationalUnit();
        organisationalUnit.setCode(GENERIC_ORG_CODE);
        return organisationalUnit;
    }

    private AgencyToken generateBasicAgencyToken() {
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setAgencyDomains(Collections.singletonList(new Domain(1L, GENERIC_DOMAIN)));
        return agencyToken;
    }


    /*
    /signup/request
     */

    @Test
    public void shouldReturnCreateAccountForm() throws Exception {
        mockMvc.perform(
                get("/signup/request")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(REQUEST_INVITE_TEMPLATE))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("id=\"confirmEmail\"")));
    }

    @Test
    public void shouldConfirmInviteSentIfAllowListedDomainAndNotAgency() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(inviteService.getInviteForEmailAndStatus(email, PENDING)).thenReturn(Optional.empty());
        when(identityService.isIdentityExistsForEmail(email)).thenReturn(false);
        when(identityService.isDomainInAnAgencyToken(domain)).thenReturn(false);
        when(identityService.isDomainAllowListed(domain)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().isOk())
                .andExpect(view().name(INVITE_SENT_TEMPLATE))
                .andExpect(content().string(containsString("Check your email")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("If an account with the email address")))
                .andExpect(content().string(containsString("does not already exist then you will receive " +
                        "an email with the link to create the account.")))
                .andExpect(content().string(containsString(
                        "Please check your emails (including the junk/spam folder) for a link to " +
                                "<strong>continue creating your account</strong>.")))
                .andExpect(content().string(containsString("The link will expire in <span>3 days</span>.")))
                .andExpect(model().attributeExists("emailId"))
                .andExpect(content().string(containsString(email)))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());

        verify(inviteService).sendSelfSignupInvite(email, true);
    }

    @Test
    public void shouldExpireInviteIfUserReRegAfterRegAllowedTimeButBeforeActivationLinkExpire() throws Exception {
        Invite invite = generateBasicInvite(true);
        invite.setInvitedAt(now(clock).minusDays(3));

        when(inviteService.getInviteForEmailAndStatus(GENERIC_EMAIL, PENDING)).thenReturn(Optional.of(invite));
        when(inviteService.isInviteExpired(invite)).thenReturn(false);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", GENERIC_EMAIL)
                        .param("confirmEmail", GENERIC_EMAIL)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP_REQUEST));

        verify(inviteService, times(1)).updateInviteStatus(invite.getCode(), EXPIRED);
    }

    @Test
    public void shouldFailValidationIfEmailAddressIsNotValid() throws Exception {
        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", "userDomain.org")
                        .param("confirmEmail", "userDomain.org")
                )
                .andExpect(status().isOk())
                .andExpect(view().name(REQUEST_INVITE_TEMPLATE))
                .andExpect(content().string(containsString("Email address is not valid")));
    }

    @Test
    public void shouldRedirectToSignupIfPreviousInviteHasPassedTheValidityDuration() throws Exception {

        Invite invite = generateBasicInvite(true);
        invite.setInvitedAt(now(clock).minusDays(7));
        when(inviteService.getInviteForEmailAndStatus(GENERIC_EMAIL, PENDING)).thenReturn(Optional.of(invite));

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", GENERIC_EMAIL)
                        .param("confirmEmail", GENERIC_EMAIL)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP_REQUEST));

        verify(inviteService, times(1)).updateInviteStatus(invite.getCode(), EXPIRED);
    }

    @Test
    public void shouldRedirectToPendingSignupIfPreviousInviteIsValid() throws Exception {

        Invite invite = generateBasicInvite(true);
        invite.setInvitedAt(now(clock));
        when(inviteService.getInviteForEmailAndStatus(GENERIC_EMAIL, PENDING)).thenReturn(Optional.of(invite));

        mockMvc.perform(
                        post("/signup/request")
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("email", GENERIC_EMAIL)
                                .param("confirmEmail", GENERIC_EMAIL)
                )
                .andExpect(status().isOk())
                .andExpect(view().name(PENDING_SIGNUP_TEMPLATE))
                .andExpect(content().string(containsString("Create account pending")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("Check your email for the link to register your account.")))
                .andExpect(content().string(containsString("You have been sent an email with a link to register your account.")))
                .andExpect(content().string(containsString("Please check your emails (including the junk/spam folder).")))
                .andExpect(content().string(containsString("Haven't received the email?")))
                .andExpect(content().string(containsString("Check your spam folder.")))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());
    }

    @Test
    public void shouldShowInviteSentScreenIfUserAlreadyExists() throws Exception {

        String email = "user@domain.com";
        when(inviteService.getInviteForEmailAndStatus(email, PENDING)).thenReturn(Optional.empty());
        when(identityService.isIdentityExistsForEmail(email)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().isOk())
                .andExpect(view().name(INVITE_SENT_TEMPLATE))
                .andExpect(content().string(containsString("Check your email")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("If an account with the email address")))
                .andExpect(content().string(containsString("does not already exist then you will receive " +
                        "an email with the link to create the account.")))
                .andExpect(content().string(containsString(
                        "Please check your emails (including the junk/spam folder) for a link to " +
                                "<strong>continue creating your account</strong>.")))
                .andExpect(content().string(containsString("The link will expire in <span>3 days</span>.")))
                .andExpect(model().attributeExists("emailId"))
                .andExpect(content().string(containsString(email)))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());
    }

    @Test
    public void shouldConfirmInviteSentIfAgencyTokenEmail() throws Exception {
        when(inviteService.getInviteForEmailAndStatus(GENERIC_EMAIL, PENDING)).thenReturn(Optional.empty());
        when(identityService.isIdentityExistsForEmail(GENERIC_EMAIL)).thenReturn(false);
        when(identityService.isDomainInAnAgencyToken(GENERIC_DOMAIN)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", GENERIC_EMAIL)
                        .param("confirmEmail", GENERIC_EMAIL)
                )
                .andExpect(status().isOk())
                .andExpect(view().name(INVITE_SENT_TEMPLATE))
                .andExpect(content().string(containsString("Check your email")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("If an account with the email address")))
                .andExpect(content().string(containsString("does not already exist then you will receive " +
                        "an email with the link to create the account.")))
                .andExpect(content().string(containsString(
                        "Please check your emails (including the junk/spam folder) for a link to " +
                                "<strong>continue creating your account</strong>.")))
                .andExpect(content().string(containsString("The link will expire in <span>3 days</span>.")))
                .andExpect(model().attributeExists("emailId"))
                .andExpect(content().string(containsString(GENERIC_EMAIL)))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());

        verify(inviteService).sendSelfSignupInvite(GENERIC_EMAIL, false);
    }

    @Test
    public void shouldNotSendInviteIfNotAllowListedAndNotAgencyTokenEmail() throws Exception {
        when(inviteService.getInviteForEmailAndStatus(GENERIC_EMAIL, PENDING)).thenReturn(Optional.empty());
        when(identityService.isIdentityExistsForEmail(GENERIC_EMAIL)).thenReturn(false);
        when(identityService.isDomainAllowListed(GENERIC_DOMAIN)).thenReturn(false);
        when(identityService.isDomainInAnAgencyToken(GENERIC_DOMAIN)).thenReturn(false);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", GENERIC_EMAIL)
                        .param("confirmEmail", GENERIC_EMAIL)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP_REQUEST))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE,
                        "Your organisation is unable to use this service. Please contact your line manager."));
    }

    /*
    /signup/
     */

    @Test
    public void shouldRedirectToSignupIfInviteCodeDoesNotExists() throws Exception {
        when(inviteService.isInviteCodeExists(GENERIC_CODE)).thenReturn(false);

        mockMvc.perform(
                        get("/signup/" + GENERIC_CODE)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP_REQUEST));
    }

    @Test
    public void shouldRedirectToSignupIfSignupCodeAlreadyUsed() throws Exception {
        when(inviteService.isInviteCodeExists(GENERIC_CODE)).thenReturn(true);
        when(inviteService.isInviteCodeUsed(GENERIC_CODE)).thenReturn(true);

        mockMvc.perform(
                        get("/signup/" + GENERIC_CODE)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP_REQUEST));
    }

    @Test
    public void shouldRedirectToSignupIfInviteCodeExpired() throws Exception {
        when(inviteService.isInviteCodeExists(GENERIC_CODE)).thenReturn(true);
        when(inviteService.isInviteCodeExpired(GENERIC_CODE)).thenReturn(true);

        mockMvc.perform(
                        get("/signup/" + GENERIC_CODE)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP_REQUEST));
    }

    @Test
    public void shouldRedirectToEnterTokenPageIfInviteNotAuthorised() throws Exception {
        Invite invite = generateBasicInvite(false);

        when(inviteService.isInviteCodeExists(GENERIC_CODE)).thenReturn(true);
        when(inviteService.isInviteCodeUsed(GENERIC_CODE)).thenReturn(false);
        when(inviteService.isInviteCodeExpired(GENERIC_CODE)).thenReturn(false);
        when(inviteService.getInviteForCode(GENERIC_CODE)).thenReturn(invite);

        mockMvc.perform(
                get("/signup/" + GENERIC_CODE)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_CHOOSE_ORGANISATION + GENERIC_CODE));
    }

    @Test
    public void shouldReturnSignupIfInviteAuthorised() throws Exception {
        Invite invite = generateBasicInvite(true);

        when(inviteService.isInviteCodeExists(GENERIC_CODE)).thenReturn(true);
        when(inviteService.isInviteCodeUsed(GENERIC_CODE)).thenReturn(false);
        when(inviteService.isInviteCodeExpired(GENERIC_CODE)).thenReturn(false);
        when(inviteService.getInviteForCode(GENERIC_CODE)).thenReturn(invite);

        mockMvc.perform(
                get("/signup/" + GENERIC_CODE)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(SIGNUP_TEMPLATE));
    }

    @Test
    public void shouldReturnSignupSuccessIfInviteAuthorised() throws Exception {
        String password = "Password1";
        Invite invite = generateBasicInvite(true);

        AgencyToken agencyToken = new AgencyToken();

        when(inviteService.isInviteCodeValid(GENERIC_CODE)).thenReturn(true);
        when(inviteService.getInviteForCode(GENERIC_CODE)).thenReturn(invite);
        doNothing().when(identityService).createIdentityFromInviteCode(GENERIC_CODE, password, agencyToken);
        doNothing().when(inviteService).updateInviteStatus(GENERIC_CODE, InviteStatus.ACCEPTED);

        mockMvc.perform(
                post("/signup/" + GENERIC_CODE)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                        .flashAttr("exampleEntity", agencyToken)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name(SIGNUP_SUCCESS_TEMPLATE));
    }

    @Test
    public void shouldRedirectToPasswordSignupIfExceptionThrown() throws Exception {
        String password = "Password1";
        Invite invite = generateBasicInvite(true);
        AgencyToken agencyToken = new AgencyToken();

        when(inviteService.isInviteCodeValid(GENERIC_CODE)).thenReturn(true);
        when(inviteService.getInviteForCode(GENERIC_CODE)).thenReturn(invite);
        doThrow(new UnableToAllocateAgencyTokenException("Error")).when(identityService)
                .createIdentityFromInviteCode(GENERIC_CODE, password, agencyToken);

        mockMvc.perform(
                post("/signup/" + GENERIC_CODE)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                        .flashAttr("exampleEntity", agencyToken)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP + GENERIC_CODE));
    }

    /*
    /signup/chooseOrganisation/
     */

    @Test
    public void enterOrganisationRedirectToLoginWhenInviteIsInvalid() throws Exception {
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(null);
        mockMvc.perform(
                        get("/signup/chooseOrganisation/" + GENERIC_CODE)
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_INVALID_SIGNUP_CODE));
    }

    @Test
    public void enterOrganisationRedirectToSignUpWhenInviteIsAuthorised() throws Exception {
        Invite invite = generateBasicInvite(true);
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        mockMvc.perform(
                        get("/signup/chooseOrganisation/" + GENERIC_CODE)
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + GENERIC_CODE));
    }

    @Test
    public void enterOrganisationShouldRenderTemplateWhenInviteIsValid() throws Exception {
        OrganisationalUnit org = generateBasicOrganisation();
        org.setAgencyToken(generateBasicAgencyToken());
        List<OrganisationalUnit> orgs = Collections.singletonList(org);

        Invite invite = generateBasicInvite(false);
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.getOrganisationalUnitsByDomain(GENERIC_DOMAIN)).thenReturn(orgs);

        mockMvc.perform(
                        get("/signup/chooseOrganisation/" + GENERIC_CODE)
                                .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name(CHOOSE_ORGANISATION_TEMPLATE));
    }

    @Test
    public void chooseOrganisationShouldRedirectToEnterTokenIfAgency() throws Exception {
        OrganisationalUnit org = generateBasicOrganisation();
        org.setAgencyToken(generateBasicAgencyToken());
        List<OrganisationalUnit> orgs = Collections.singletonList(org);
        Invite invite = generateBasicInvite(false);
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.getOrganisationalUnitsByDomain(GENERIC_DOMAIN)).thenReturn(orgs);
        mockMvc.perform(
                        post("/signup/chooseOrganisation/" + GENERIC_CODE)
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("organisation", GENERIC_ORG_CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(String.format("/signup/enterToken/%s/%s", GENERIC_CODE, GENERIC_ORG_CODE)));
    }

    @Test
    public void chooseOrganisationShouldAuthoriseInviteAndRedirectToSignupIfAllowlist() throws Exception {
        OrganisationalUnit org = generateBasicOrganisation();
        org.setDomains(Collections.singletonList(new Domain(1L, GENERIC_DOMAIN)));
        List<OrganisationalUnit> orgs = Collections.singletonList(org);
        Invite invite = generateBasicInvite(false);
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.getOrganisationalUnitsByDomain(GENERIC_DOMAIN)).thenReturn(orgs);

        mockMvc.perform(
                        post("/signup/chooseOrganisation/" + GENERIC_CODE)
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("organisation", GENERIC_ORG_CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(String.format("/signup/%s", GENERIC_CODE)));
    }

    /*
    /signup/enterToken/
     */

    private String enterTokenUrl() {
        return REDIRECT_ENTER_TOKEN + GENERIC_CODE + "/" + GENERIC_ORG_CODE;
    }

    @Test
    public void shouldRedirectToLoginIfInviteNotValidFromToken() throws Exception {
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(null);

        mockMvc.perform(
                        get(enterTokenUrl())
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_INVALID_SIGNUP_CODE));
    }

    @Test
    public void shouldRedirectOnEnterTokenIfTokenAuthorised() throws Exception {
        Invite invite = generateBasicInvite(true);
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);

        mockMvc.perform(
                        get(enterTokenUrl())
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_SIGNUP + GENERIC_CODE));
    }

    @Test
    public void shouldReturnEnterToken() throws Exception {
        Invite invite = generateBasicInvite(false);
        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.isDomainInAnAgencyTokenWithOrg(GENERIC_DOMAIN, GENERIC_ORG_CODE))
                .thenReturn(true);

        mockMvc.perform(
                        get(enterTokenUrl())
                                .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name(ENTER_TOKEN_TEMPLATE));
    }

    @Test
    public void shouldRedirectToLoginIfTokenInviteInvalid() throws Exception {
        String token = "token123";

        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(null);

        mockMvc.perform(
                        post(enterTokenUrl())
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_INVALID_SIGNUP_CODE));
    }

    @Test
    public void shouldRedirectToSignupIfInviteValidAndAgencyTokenHasSpaceAvailable() throws Exception {
        String token = "token123";

        Invite invite = generateBasicInvite(true);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setCapacity(10L);
        Optional<AgencyToken> optionalAgencyToken = Optional.of(agencyToken);

        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.getAgencyToken(GENERIC_DOMAIN, token, GENERIC_ORG_CODE))
                .thenReturn(optionalAgencyToken);
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(true);

        mockMvc.perform(
                        post(enterTokenUrl())
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + GENERIC_CODE
                        + "?" + SKIP_MAINTENANCE_PAGE_PARAM_NAME + "=" + GENERIC_EMAIL));
    }

    @Test
    public void shouldRedirectToTokenWithErrorIfInviteValidAndAgencyTokenDoesNotHaveSpaceAvailable()
            throws Exception {
        String token = "token123";

        Invite invite = generateBasicInvite(true);
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setCapacity(10L);
        Optional<AgencyToken> optionalAgencyToken = Optional.of(agencyToken);

        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.getAgencyToken(GENERIC_DOMAIN, token, GENERIC_ORG_CODE))
                .thenReturn(optionalAgencyToken);
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(false);

        mockMvc.perform(
                        post(enterTokenUrl())
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("organisation", GENERIC_ORG_CODE)
                                .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(enterTokenUrl()));
    }

    @Test
    public void shouldRedirectToEnterTokenWithErrorMessageIfNoTokensFound() throws Exception {
        String token = "token123";

        Invite invite = generateBasicInvite(true);
        Optional<AgencyToken> emptyOptional = Optional.empty();

        when(inviteService.getValidInviteForCode(GENERIC_CODE)).thenReturn(invite);
        when(csrsService.getAgencyToken(GENERIC_DOMAIN, token, GENERIC_ORG_CODE))
                .thenReturn(emptyOptional);

        mockMvc.perform(
                        post(enterTokenUrl())
                                .with(csrf())
                                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                                .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(enterTokenUrl()))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, ENTER_TOKEN_ERROR_MESSAGE));
    }
}
