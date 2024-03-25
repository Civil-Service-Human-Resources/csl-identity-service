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
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnitDTO;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.service.AgencyTokenCapacityService;
import uk.gov.cabinetoffice.csl.service.InviteService;

import java.util.Date;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.domain.InviteStatus.EXPIRED;
import static uk.gov.cabinetoffice.csl.domain.InviteStatus.PENDING;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.ENTER_TOKEN_ERROR_MESSAGE;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class SignupControllerTest {

    private static final String STATUS_ATTRIBUTE = "status";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICivilServantRegistryClient civilServantRegistryClient;

    @MockBean
    private InviteService inviteService;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @Test
    public void shouldReturnCreateAccountForm() throws Exception {
        mockMvc.perform(
                get("/signup/request")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("id=\"confirmEmail\"")));
    }

    @Test
    public void shouldConfirmInviteSentIfAllowListedDomainAndNotAgency() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(identityService.isIdentityExistsForEmail(email)).thenReturn(false);
        when(identityService.isAllowListedDomain(domain)).thenReturn(true);
        when(identityService.isDomainInAgency(domain)).thenReturn(false);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("We've sent you an email")))
                .andExpect(content().string(containsString("What happens next")))
                .andExpect(content().string(containsString(
                        "We have sent you an email with a link to <strong>continue creating your account</strong>.")));

        verify(inviteService).sendSelfSignupInvite(email, true);
    }

    @Test
    public void shouldExpireInviteIfUserReRegAfterRegAllowedTimeButBeforeActivationLinkExpire() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";
        Optional<Invite> invite = Optional.of(new Invite());
        invite.get().setInvitedAt(new Date(System.currentTimeMillis() - 25*60*60*1000));
        invite.get().setCode("code");

        when(inviteService.getInviteForEmailAndStatus(email, PENDING)).thenReturn(invite);
        when(inviteService.isInviteExpired(invite.get())).thenReturn(false);
        when(identityService.isAllowListedDomain(domain)).thenReturn(true);
        when(identityService.isDomainInAgency(domain)).thenReturn(false);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().isOk());

        verify(inviteService, times(1)).updateInviteStatus(invite.get().getCode(), EXPIRED);
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
                .andExpect(content().string(containsString("Email address is not valid")));
    }

    @Test
    public void shouldRedirectToSignupIfUserHasAlreadyBeenInvited() throws Exception {
        String email = "user@domain.com";

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void shouldRedirectToSignupIfUserAlreadyExists() throws Exception {
        String email = "user@domain.com";

        when(identityService.isIdentityExistsForEmail(email)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldConfirmInviteSentIfAgencyTokenEmail() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(identityService.isIdentityExistsForEmail(email)).thenReturn(false);
        when(identityService.isDomainInAgency(domain)).thenReturn(true);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().isOk())
                .andExpect(view().name("inviteSent"))
                .andExpect(content().string(containsString("We've sent you an email")))
                .andExpect(content().string(containsString("What happens next")))
                .andExpect(content().string(containsString(
                        "We have sent you an email with a link to <strong>continue creating your account</strong>.")));

        verify(inviteService).sendSelfSignupInvite(email, false);
    }

    @Test 
    public void shouldNotSendInviteIfNotAllowListedAndNotAgencyTokenEmail() throws Exception {
        String email = "user@domain.com";
        String domain = "domain.com";

        when(identityService.isIdentityExistsForEmail(email)).thenReturn(false);
        when(identityService.isAllowListedDomain(domain)).thenReturn(false);
        when(identityService.isDomainInAgency(domain)).thenReturn(false);

        mockMvc.perform(
                post("/signup/request")
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("email", email)
                        .param("confirmEmail", email)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE,
                        "Your organisation is unable to use this service. Please contact your line manager."));
    }

    @Test
    public void shouldRedirectToSignupIfSignupCodeNotValid() throws Exception {
        String code = "abc123";

        when(inviteService.isInviteValid(code)).thenReturn(false);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldRedirectToSignupIfInviteCodeExpired() throws Exception {
        String code = "abc123";

        when(inviteService.isInviteCodeExists(code)).thenReturn(true);
        when(inviteService.isInviteCodeExpired(code)).thenReturn(true);
        doNothing().when(inviteService).updateInviteStatus(code, EXPIRED);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldRedirectToSignupIfInviteCodeDoesNotExists() throws Exception {
        String code = "abc123";

        when(inviteService.isInviteCodeExists(code)).thenReturn(false);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/request"));
    }

    @Test
    public void shouldRedirectToEnterTokenPageIfInviteNotAuthorised() throws Exception {
        String code = "abc123";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(false);

        when(inviteService.isInviteCodeExists(code)).thenReturn(true);
        when(inviteService.isInviteCodeExpired(code)).thenReturn(false);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code));
    }

    @Test
    public void shouldReturnSignupIfInviteAuthorised() throws Exception {
        String code = "abc123";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(true);

        when(inviteService.isInviteCodeExists(code)).thenReturn(true);
        when(inviteService.isInviteCodeExpired(code)).thenReturn(false);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);

        mockMvc.perform(
                get("/signup/" + code)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test 
    public void shouldRedirectToSignUpIfFormHasError() throws Exception {
        String code = "abc123";
        String password = "password";

        when(inviteService.isInviteValid(code)).thenReturn(false);
        when(inviteService.getInviteForCode(anyString())).thenReturn(new Invite());

        mockMvc.perform(
                post("/signup/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", "doesn't match")
                )
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
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                )
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
        when(inviteService.getInviteForCode(code)).thenReturn(invite);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code));
    }

    @Test
    public void shouldReturnSignupSuccessIfInviteAuthorised() throws Exception {
        String code = "abc123";
        String password = "Password1";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(true);
        AgencyToken agencyToken = new AgencyToken();

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        doNothing().when(identityService).createIdentityFromInviteCode(code, password, agencyToken);
        doNothing().when(inviteService).updateInviteStatus(code, InviteStatus.ACCEPTED);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                        .flashAttr("exampleEntity", agencyToken)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("signupSuccess"));
    }

    @Test
    public void shouldRedirectToPasswordSignupIfExceptionThrown() throws Exception {
        String code = "abc123";
        String password = "Password1";
        Invite invite = new Invite();
        invite.setAuthorisedInvite(true);
        AgencyToken agencyToken = new AgencyToken();

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        doThrow(new UnableToAllocateAgencyTokenException("Error")).when(identityService)
                .createIdentityFromInviteCode(code, password, agencyToken);

        mockMvc.perform(
                post("/signup/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("password", password)
                        .param("confirmPassword", password)
                        .flashAttr("exampleEntity", agencyToken)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + code));
    }

    @Test
    public void shouldRedirectToLoginIfInviteNotValidFromToken() throws Exception {
        String code = "abc123";

        when(inviteService.isInviteValid(code)).thenReturn(false);

        mockMvc.perform(
                get("/signup/enterToken/" + code)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void shouldReturnEnterToken() throws Exception {
        String code = "abc123";
        String email = "test@example.com";

        OrganisationalUnitDTO[] organisationalUnits = new OrganisationalUnitDTO[]{new OrganisationalUnitDTO()};

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(false);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getOrganisationalUnitsFormatted()).thenReturn(organisationalUnits);

        mockMvc.perform(
                get("/signup/enterToken/" + code)
                        .with(csrf())
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

        OrganisationalUnitDTO[] organisationalUnits = new OrganisationalUnitDTO[]{new OrganisationalUnitDTO()};

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getOrganisationalUnitsFormatted()).thenReturn(organisationalUnits);

        mockMvc.perform(
                get("/signup/enterToken/" + code)
                        .with(csrf())
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
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token)
                )
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
        agencyToken.setCapacity(10L);
        Optional<AgencyToken> optionalAgencyToken = Optional.of(agencyToken);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(domain, token, organisation))
                .thenReturn(optionalAgencyToken);
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(true);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/" + code));
    }

    @Test
    public void shouldRedirectToTokenWithErrorIfInviteValidAndAgencyTokenDoesNotHaveSpaceAvailable()
            throws Exception {
        String code = "abc123";
        String organisation = "org";
        String token = "token123";
        String email = "test@example.com";
        String domain = "example.com";

        Invite invite = new Invite();
        invite.setForEmail(email);
        invite.setAuthorisedInvite(true);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setCapacity(10L);
        Optional<AgencyToken> optionalAgencyToken = Optional.of(agencyToken);

        when(inviteService.isInviteValid(code)).thenReturn(true);
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(domain, token, organisation))
                .thenReturn(optionalAgencyToken);
        when(agencyTokenCapacityService.hasSpaceAvailable(agencyToken)).thenReturn(false);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token)
                )
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
        when(inviteService.getInviteForCode(code)).thenReturn(invite);
        when(civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(domain, token, organisation))
                .thenReturn(emptyOptional);

        mockMvc.perform(
                post("/signup/enterToken/" + code)
                        .with(csrf())
                        .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                        .param("organisation", organisation)
                        .param("token", token)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/enterToken/" + code))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, ENTER_TOKEN_ERROR_MESSAGE));
    }
}
