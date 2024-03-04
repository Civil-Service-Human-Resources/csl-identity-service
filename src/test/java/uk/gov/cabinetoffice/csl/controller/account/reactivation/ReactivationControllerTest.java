package uk.gov.cabinetoffice.csl.controller.account.reactivation;

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
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.service.AgencyTokenService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.STATUS_ATTRIBUTE;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class ReactivationControllerTest {

    private static final String CODE = "abc123";
    private static final String EMAIL_ADDRESS = "test@example.com";
    private static final String DOMAIN = "example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReactivationService reactivationService;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private AgencyTokenService agencyTokenService;

    @MockBean
    private Utils utils;

    @Test
    public void shouldRedirectIfAccountDomainIsAgencyToken() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();

        when(reactivationService.isReactivationExpired(reactivation)).thenReturn(false);
        when(agencyTokenService.isDomainInAgencyToken(DOMAIN)).thenReturn(true);

        mockMvc.perform(
                get("/account/reactivate/" + CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/verify/agency/" + CODE));
    }

    @Test
    public void shouldReactivateAccountIfNotAgencyToken() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();

        when(reactivationService.isReactivationExpired(reactivation)).thenReturn(false);
        doNothing().when(reactivationService).reactivateIdentity(reactivation);

        mockMvc.perform(
                get("/account/reactivate/" + CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/reactivate/updated"));
    }

    @Test
    public void shouldShowUserReactivationRequestHasExpired() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();

        when(reactivationService.isReactivationExpired(reactivation)).thenReturn(true);
        when(utils.validityMessage("You have %s to click the reactivation link within the email.",
                86400))
                .thenReturn("You have 24 hours to click the reactivation link within the email.");

        String encryptedUsername = "jFwK%2FMPj%2BmHqdD4q7KhcBoqjYkH96N8FTcMlxsaVuJ4%3D";

        mockMvc.perform(
                        get("/account/reactivate/" + CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=deactivated-expired" +
                        "&reactivationValidityMessage=You have 24 hours to click the reactivation link within the email." +
                        "&username=" + encryptedUsername))
                .andDo(print());
    }

    @Test
    public void shouldRedirectToLoginIfReactivationNotFound() throws Exception {

        doThrow(new ResourceNotFoundException()).when(reactivationService)
                .getReactivationForCodeAndStatus(CODE, PENDING);

        mockMvc.perform(
                get("/account/reactivate/" + CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, "Reactivation code is not valid"))
                .andDo(print());
    }

    @Test
    public void shouldRedirectToLoginIfTechnicalExceptionOccurs() throws Exception {

        doThrow(new RuntimeException()).when(reactivationService).
                getReactivationForCodeAndStatus(CODE, PENDING);

        mockMvc.perform(
                get("/account/reactivate/" + CODE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE,
                        "There was an error processing account reactivation. Please try again later."))
                .andDo(print());
    }

    @Test
    public void shouldGetAccountReactivatedTemplate() throws Exception {
        mockMvc.perform(get("/account/reactivate/updated"))
                .andExpect(status().isOk())
                .andExpect(view().name("reactivate/accountReactivated"));
    }

    private Reactivation createPendingActivationAndMockServicesInvocation() throws Exception {
        Identity identity = new Identity();
        identity.setEmail(EMAIL_ADDRESS);
        identity.setActive(false);
        when(identityService.getIdentityForEmail(EMAIL_ADDRESS)).thenReturn(identity);

        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(EMAIL_ADDRESS);
        reactivation.setCode(CODE);
        reactivation.setReactivationStatus(PENDING);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("01-Feb-2024");
        reactivation.setRequestedAt(dateOfReactivationRequest);

        when(reactivationService.getReactivationForCodeAndStatus(CODE, PENDING))
                .thenReturn(reactivation);
        when(utils.getDomainFromEmailAddress(EMAIL_ADDRESS)).thenReturn(DOMAIN);

        return reactivation;
    }
}
