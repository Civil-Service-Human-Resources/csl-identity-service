package uk.gov.cabinetoffice.csl.controller.reactivation;

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
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.NotifyService;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.Month.FEBRUARY;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getEncryptedText;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class ReactivationControllerTest {

    private static final String CODE = "abc123";
    private static final String EMAIL = "test@example.com";
    private final int reactivationValidityInSeconds = 86400;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReactivationService reactivationService;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private NotifyService notifyService;

    private final Utils utils = new Utils();

    @Test
    public void shouldCreatePendingReactivationAndSendEmailIfNoPendingReactivation() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();
        String title = "Check your email";
        String viewName = "reactivate/reactivate";
        String reactivationEmailMessage = "We&#39;ve sent you an email with a link to reactivate your account.";
        String reactivationValidityMessage = "You have %s to click the reactivation link within the email."
                .formatted(utils.convertSecondsIntoDaysHoursMinutesSeconds(reactivationValidityInSeconds));

        executeSendReactivationEmail(reactivation, false, reactivationEmailMessage,
                reactivationValidityMessage, title, viewName);
    }

    @Test
    public void shouldNotCreatePendingReactivationAndNotSendEmailIfPendingReactivationExists() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();
        LocalDateTime requestedAt = LocalDateTime.now();
        reactivation.setRequestedAt(requestedAt);
        String title = "Account reactivation pending";
        String viewName = "reactivate/pendingReactivate";
        String reactivationEmailMessage = ("We&#39;ve sent you an email on %s with a link to reactivate your " +
                "account.").formatted(utils.convertDateTimeFormat(requestedAt.toString()));
        LocalDateTime reactivationLinkExpiryDateTime = requestedAt.plusSeconds(reactivationValidityInSeconds);
        String reactivationValidityMessage = ("The link in the email will expire on %s after which you will be " +
                "able to request a new link by repeating the reactivation process on the login page.")
                .formatted(utils.convertDateTimeFormat(reactivationLinkExpiryDateTime.toString()));

        executeSendReactivationEmail(reactivation, true, reactivationEmailMessage,
                reactivationValidityMessage, title, viewName);
    }

    @Test
    public void shouldRedirectIfAccountDomainIsAgencyToken() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();

        when(reactivationService.isReactivationExpired(reactivation)).thenReturn(false);
        when(identityService.isDomainInAnAgencyToken(utils.getDomainFromEmailAddress(EMAIL))).thenReturn(true);

        mockMvc.perform(
                get("/account/reactivate/" + CODE)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/verify/agency/" + CODE));
    }

    @Test
    public void shouldReactivateAccountIfNotAgencyToken() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();

        when(reactivationService.isReactivationExpired(reactivation)).thenReturn(false);
        doNothing().when(reactivationService).reactivateIdentity(reactivation);

        mockMvc.perform(
                get("/account/reactivate/" + CODE)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/reactivate/updated"));
    }

    @Test
    public void shouldShowUserReactivationRequestHasExpired() throws Exception {
        Reactivation reactivation = createPendingActivationAndMockServicesInvocation();

        when(reactivationService.isReactivationExpired(reactivation)).thenReturn(true);

        String encryptedUsername = "jFwK%2FMPj%2BmHqdD4q7KhcBoqjYkH96N8FTcMlxsaVuJ4%3D";

        mockMvc.perform(
                        get("/account/reactivate/" + CODE)
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=reactivation-expired&username=" + encryptedUsername))
                .andDo(print());
    }

    @Test
    public void shouldRedirectToLoginIfReactivationNotFound() throws Exception {

        doThrow(new ResourceNotFoundException(PENDING + " Reactivation not found for code: " + CODE))
                .when(reactivationService).getReactivationForCodeAndStatus(CODE, PENDING);

        mockMvc.perform(
                get("/account/reactivate/" + CODE)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, REACTIVATION_CODE_IS_NOT_VALID_ERROR_MESSAGE))
                .andDo(print());
    }

    @Test
    public void shouldRedirectToLoginIfTechnicalExceptionOccurs() throws Exception {

        doThrow(new RuntimeException()).when(reactivationService).
                getReactivationForCodeAndStatus(CODE, PENDING);

        mockMvc.perform(
                get("/account/reactivate/" + CODE)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, ACCOUNT_REACTIVATION_ERROR_MESSAGE))
                .andDo(print());
    }

    @Test
    public void shouldGetAccountReactivatedTemplate() throws Exception {
        mockMvc.perform(get("/account/reactivate/updated")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("reactivate/accountReactivated"))
                .andExpect(content().string(containsString("Account reactivation final step")))
                .andExpect(content().string(containsString("Your account will be successfully reactivated once you log in")));
    }

    private void executeSendReactivationEmail(Reactivation reactivation,
                                              boolean isPendingReactivation,
                                              String reactivationEmailMessage,
                                              String reactivationValidityMessage,
                                              String title,
                                              String viewName)  throws Exception {
        when(reactivationService.isPendingReactivationExistsForEmail(EMAIL)).thenReturn(isPendingReactivation);
        when(reactivationService.createPendingReactivation(EMAIL)).thenReturn(reactivation);
        when(reactivationService.getPendingReactivationForEmail(EMAIL)).thenReturn(reactivation);

        Map<String, String> emailPersonalisation = new HashMap<>();
        emailPersonalisation.put("learnerName", EMAIL);
        emailPersonalisation.put("reactivationUrl", "/account/reactivate/" + reactivation.getCode());
        doNothing().when(notifyService).notifyWithPersonalisation(reactivation.getEmail(),
                "reactivationEmailTemplateId", emailPersonalisation);

        String encryptionKey = "0123456789abcdef0123456789abcdef";
        String encryptedUsername = getEncryptedText(EMAIL, encryptionKey);

        mockMvc.perform(
                    get("/account/reactivate")
                        .param("code", encryptedUsername)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(viewName))
                .andExpect(content().string(containsString(title)))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("Check your email for the link to reactivate your account.")))
                .andExpect(content().string(containsString(reactivationEmailMessage)))
                .andExpect(content().string(containsString(reactivationValidityMessage)))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());
    }

    private Reactivation createPendingActivationAndMockServicesInvocation() {
        Identity identity = new Identity();
        identity.setEmail(EMAIL);
        identity.setActive(false);
        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);

        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(EMAIL);
        reactivation.setCode(CODE);
        reactivation.setReactivationStatus(PENDING);
        LocalDateTime dateOfReactivationRequest = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        reactivation.setRequestedAt(dateOfReactivationRequest);

        when(reactivationService.getReactivationForCodeAndStatus(CODE, PENDING))
                .thenReturn(reactivation);

        return reactivation;
    }
}
