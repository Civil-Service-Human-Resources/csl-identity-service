package uk.gov.cabinetoffice.csl.controller.signup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.repository.InviteRepository;
import uk.gov.cabinetoffice.csl.service.AgencyTokenCapacityService;
import uk.gov.cabinetoffice.csl.service.CsrsService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.InviteService;
import uk.gov.cabinetoffice.csl.util.ApplicationConstants;
import uk.gov.cabinetoffice.csl.utils.CsrfRequestPostProcessor;

import java.util.Date;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WithMockUser(username = "user")
public class SignupControllerTest {

    private static final String STATUS_ATTRIBUTE = "status";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InviteService inviteService;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private CsrsService csrsService;

    @MockBean
    private InviteRepository inviteRepository;

    @MockBean
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @Test
    public void shouldReturnCreateAccountForm() throws Exception {
        mockMvc.perform(
                get("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("id=\"confirmEmail\"")));
    }

    @Test
    public void shouldConfirmInviteSentIfWhitelistedDomainAndNotAgency() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(inviteRepository.existsByForEmailAndStatus(email, InviteStatus.PENDING)).thenReturn(false);
        when(identityService.existsByEmail(email)).thenReturn(false);
        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(identityService.isWhitelistedDomain(domain)).thenReturn(true);
        when(csrsService.isDomainInAgency(domain)).thenReturn(false);
        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("We've sent you an email")))
                .andExpect(content().string(containsString("What happens next")))
                .andExpect(content().string(containsString("We have sent you an email with a link to <strong>continue creating your account</strong>.")));

        verify(inviteService).sendSelfSignupInvite(email, true);
    }

    @Test
    public void shouldExpireInviteIfUserReregAfterRegAllowedTimeButBeforeActivationLinkExpire() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";
        Optional<Invite> invite = Optional.of(new Invite());
        invite.get().setInvitedAt(new Date(System.currentTimeMillis() - 25*60*60*1000));
        invite.get().setCode("code");

        when(inviteService.findByForEmailAndStatus(email, InviteStatus.PENDING)).thenReturn(invite);
        when(inviteService.isInviteCodeExpired(invite.get())).thenReturn(false);

        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(identityService.isWhitelistedDomain(domain)).thenReturn(true);
        when(csrsService.isDomainInAgency(domain)).thenReturn(false);
        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email))
                .andExpect(status().isOk());

        verify(inviteService, times(1)).updateInviteByCode(invite.get().getCode(), InviteStatus.EXPIRED);
    }

    @Test
    public void shouldFailValidationIfEmailAddressesDoNotMatch() throws Exception {
        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", "user@domain.org")
                        .param("confirmEmail", "user1@domain.org"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email addresses do not match")));
    }

    @Test
    public void shouldFailValidationIfEmailAddressIsNotValid() throws Exception {
        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", "userdomain.org")
                        .param("confirmEmail", "userdomain.org"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email address is not valid")));
    }

    @Test
    public void shouldRedirectToSignupIfUserHasAlreadyBeenInvited() throws Exception {
        String email = "user@domain.com";

        when(inviteRepository.existsByForEmailAndStatus(email, InviteStatus.PENDING)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void shouldRedirectToSignupIfUserAlreadyExists() throws Exception {
        String email = "user@domain.com";

        when(inviteRepository.existsByForEmailAndStatus(email, InviteStatus.PENDING)).thenReturn(false);
        when(identityService.checkEmailExists(email)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldConfirmInviteSentIfAgencyTokenEmail() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(inviteRepository.existsByForEmailAndStatus(email, InviteStatus.PENDING)).thenReturn(false);
        when(identityService.existsByEmail(email)).thenReturn(false);
        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(csrsService.isDomainInAgency(domain)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email))
                .andExpect(status().isOk())
                .andExpect(view().name("inviteSent"))
                .andExpect(content().string(containsString("We've sent you an email")))
                .andExpect(content().string(containsString("What happens next")))
                .andExpect(content().string(containsString("We have sent you an email with a link to <strong>continue creating your account</strong>.")));

        verify(inviteService).sendSelfSignupInvite(email, false);
    }

    @Test 
    public void shouldNotSendInviteIfNotWhitelistedAndNotAgencyTokenEmail() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(inviteRepository.existsByForEmailAndStatus(email, InviteStatus.PENDING)).thenReturn(false);
        when(identityService.existsByEmail(email)).thenReturn(false);
        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(identityService.isWhitelistedDomain(domain)).thenReturn(false);
        when(csrsService.isDomainInAgency(domain)).thenReturn(false);

        mockMvc.perform(
                post("/signup/request")
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, "Your organisation is unable to use this service. Please contact your line manager."));
    }

    @Test
    public void shouldRedirectToSignupIfSignupCodeNotValid() throws Exception {
        String code = "abc123";

        when(inviteService.isInviteValid(code)).thenReturn(false);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldRedirectToSignupIfInviteCodeExpired() throws Exception {
        String code = "abc123";

        when(inviteService.isCodeExists(code)).thenReturn(true);
        when(inviteService.isCodeExpired(code)).thenReturn(true);
        doNothing().when(inviteService).updateInviteByCode(code, InviteStatus.EXPIRED);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldRedirectToSignupIfInviteCodeDoesNotExists() throws Exception {
        String code = "abc123";

        when(inviteService.isCodeExists(code)).thenReturn(false);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldRedirectToEnterTokenPageIfInviteNotAuthorised() throws Exception {
        String code = "abc123";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(false);

        when(inviteService.isCodeExists(code)).thenReturn(true);
        when(inviteService.isCodeExpired(code)).thenReturn(false);
        when(inviteRepository.findByCode(code)).thenReturn(invite);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code));
    }

    @Test
    public void shouldReturnSignupIfInviteAuthorised() throws Exception {
        String code = "abc123";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(true);

        when(inviteService.isCodeExists(code)).thenReturn(true);
        when(inviteService.isCodeExpired(code)).thenReturn(false);
        when(inviteRepository.findByCode(code)).thenReturn(invite);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    public void shouldNotPostIfPasswordsDifferent() throws Exception {
        String code = "abc123";
        String password = "Password1";
        String differentPassword = "differentPassword1";

        mockMvc.perform(
                post("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", differentPassword))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test 
    public void shouldRedirectToSignUpIfFormHasError() throws Exception {
        String code = "abc123";
        String password = "password";

        when(inviteService.isInviteValid(code)).thenReturn(false);
        when(inviteRepository.findByCode(anyString())).thenReturn(new Invite());

        mockMvc.perform(
                post("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", "doesn't match"))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("invite"));
    }

    @Test
    public void shouldRedirectToLoginIfInviteNotValid() throws Exception {
        String code = "abc123";
        String password = "Password1";

        when(inviteService.isInviteValid(code)).thenReturn(false);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void shouldRedirectToEnterTokenIfInviteNotAuthorised() throws Exception {
        String code = "abc123";
        String password = "Password1";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(false);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code));
    }

    @Test
    public void shouldReturnSignupSuccessIfInviteAuthorised() throws Exception {
        String code = "abc123";
        String password = "Password1";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(true);
        TokenRequest tokenRequest = new TokenRequest();

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);
        doNothing().when(identityService).createIdentityFromInviteCode(code, password, tokenRequest);
        doNothing().when(inviteService).updateInviteByCode(code, InviteStatus.ACCEPTED);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                        .flashAttr("exampleEntity", tokenRequest))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("signupSuccess"));
    }

    @Test
    public void shouldRedirectToPasswordSignupIfExceptionThrown() throws Exception {
        String code = "abc123";
        String password = "Password1";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(true);
        TokenRequest tokenRequest = new TokenRequest();

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);
        doThrow(new UnableToAllocateAgencyTokenException("Error")).when(identityService).createIdentityFromInviteCode(code, password, tokenRequest);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                        .flashAttr("exampleEntity", tokenRequest))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + code));
    }

    @Test
    public void shouldRedirectToLoginIfInviteNotValidFromToken() throws Exception {
        String code = "abc123";

        when(inviteService.isInviteValid(code)).thenReturn(false);

        mockMvc.perform(
                get("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void shouldReturnEnterToken() throws Exception {
        String code = "abc123";
        String email = "test@example.com";

        OrganisationalUnitDto[] organisationalUnits = new OrganisationalUnitDto[]{new OrganisationalUnitDto()};

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(false);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);

        when(csrsService.getOrganisationalUnitsFormatted()).thenReturn(organisationalUnits);

        mockMvc.perform(
                get("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("enterToken"));
    }

    @Test
    public void shouldRedirectOnEnterTokenIfTokenAuth() throws Exception {
        String code = "abc123";
        String email = "test@example.com";

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(true);

        OrganisationalUnitDto[] organisationalUnits = new OrganisationalUnitDto[]{new OrganisationalUnitDto()};

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);

        when(csrsService.getOrganisationalUnitsFormatted()).thenReturn(organisationalUnits);

        mockMvc.perform(
                get("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + code));
    }


    @Test
    public void shouldRedirectToLoginIfTokenInviteInvalid() throws Exception {
        String code = "abc123";
        String organisation = "org";
        String token = "token123";

        when(inviteService.isInviteValid(code)).thenReturn(false);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void shouldRedirectToSignupIfInviteValidAndAgencyTokenHasSpaceAvailable() throws Exception {
        String code = "abc123";
        String organisation = "org";
        String token = "token123";
        String email = "test@example.com";
        String domain = "example.com";

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(true);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setCapacity(10);
        Optional<AgencyToken> optionalAgencyToken = Optional.of(agencyToken);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);
        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(csrsService.getAgencyTokenForDomainTokenOrganisation(domain, token, organisation)).thenReturn(optionalAgencyToken);
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(true);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + code));
    }

    @Test
    public void shouldRedirectToTokenWithErrorIfInviteValidAndAgencyTokenDoesNotHaveSpaceAvailable() throws Exception {
        String code = "abc123";
        String organisation = "org";
        String token = "token123";
        String email = "test@example.com";
        String domain = "example.com";

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(true);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setCapacity(10);
        Optional<AgencyToken> optionalAgencyToken = Optional.of(agencyToken);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);
        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(csrsService.getAgencyTokenForDomainTokenOrganisation(domain, token, organisation)).thenReturn(optionalAgencyToken);
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(false);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code));
    }

    @Test
    public void shouldRedirectToEnterTokenWithErrorMessageIfNoTokensFound() throws Exception {
        String code = "abc123";
        String organisation = "org";
        String token = "token123";
        String email = "test@example.com";
        String domain = "example.com";

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(true);

        Optional<AgencyToken> emptyOptional = Optional.empty();

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteRepository.findByCode(code)).thenReturn(invite);
        when(identityService.getDomainFromEmailAddress(email)).thenReturn(domain);
        when(csrsService.getAgencyTokenForDomainTokenOrganisation(domain, token, organisation)).thenReturn(emptyOptional);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(CsrfRequestPostProcessor.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code))
                .andExpect(flash().attribute(ApplicationConstants.STATUS_ATTRIBUTE, ApplicationConstants.ENTER_TOKEN_ERROR_MESSAGE));
    }
}
