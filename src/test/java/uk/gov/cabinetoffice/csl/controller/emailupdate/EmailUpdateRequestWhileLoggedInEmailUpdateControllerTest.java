package uk.gov.cabinetoffice.csl.controller.emailupdate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.service.EmailUpdateService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.util.WithMockCustomUser;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("no-redis")
@WithMockCustomUser
public class EmailUpdateRequestWhileLoggedInEmailUpdateControllerTest {

    private static final String UPDATE_EMAIL_FORM_TEMPLATE = "updateEmailForm";
    private static final String UPDATE_EMAIL_VIEW_NAME_TEMPLATE = "emailupdate/updateEmail";
    private static final String EMAIL_VERIFICATION_SENT_TEMPLATE = "emailupdate/emailVerificationSent";
    private static final String PENDING_EMAIL_UPDATE_TEMPLATE = "emailupdate/pendingEmailUpdate";
    private static final String EMAIL_PATH = "/account/email";
    private static final String NEW_EMAIL = "newEmail@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private EmailUpdateService emailUpdateService;

    @Test
    public void givenARequestToChangeYourEmail_whenUpdateEmailForm_shouldDisplayForm() throws Exception {
        mockMvc.perform(get(EMAIL_PATH)
                    .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(UPDATE_EMAIL_FORM_TEMPLATE))
                .andExpect(view().name(UPDATE_EMAIL_VIEW_NAME_TEMPLATE))
                .andDo(print());
    }

    @Test
    public void givenAnEmptyForm_whenSendEmailVerification_shouldDisplayFieldValidationErrors() throws Exception {
        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", "")
                    .param("confirm", "")
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().errorCount(2))
                .andExpect(model().attributeHasFieldErrorCode(UPDATE_EMAIL_FORM_TEMPLATE, "email", "NotBlank"))
                .andExpect(model().attributeHasFieldErrorCode(UPDATE_EMAIL_FORM_TEMPLATE, "confirm", "NotBlank"))
                .andExpect(model().attributeExists(UPDATE_EMAIL_FORM_TEMPLATE))
                .andExpect(view().name(UPDATE_EMAIL_VIEW_NAME_TEMPLATE));
    }

    @Test
    public void givenAnInvalidForm_whenSendEmailVerification_shouldDisplayFieldValidationErrors() throws Exception {
        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", "someone")
                    .param("confirm", "someone")
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeExists(UPDATE_EMAIL_FORM_TEMPLATE))
                .andExpect(view().name(UPDATE_EMAIL_VIEW_NAME_TEMPLATE));
    }

    @Test
    public void givenAValidFormAndAnEmailThatAlreadyExists_whenSendEmailVerification_shouldDisplayEmailAlreadyExistsError() throws Exception {
        when(identityService.isIdentityExistsForEmail(anyString())).thenReturn(true);
        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", NEW_EMAIL)
                    .param("confirm", NEW_EMAIL)
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/email/update/error?emailAlreadyTaken=true"));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq(NEW_EMAIL));
        verify(identityService, never()).isValidEmailDomain(anyString());
    }

    @Test
    public void givenAValidFormAndAnEmailThatIsNotValid_whenSendEmailVerification_shouldDisplayUnableToUseThisServiceError() throws Exception {
        when(identityService.isValidEmailDomain(anyString())).thenReturn(false);
        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", NEW_EMAIL)
                    .param("confirm", NEW_EMAIL)
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/email/update/error?notValidEmailDomain=true"));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq(NEW_EMAIL));
        verify(identityService, times(1)).isValidEmailDomain(eq(NEW_EMAIL));
    }

    @Test
    public void givenAValidFormAndEmailDoesNotAlreadyExistAndIsAValidEmail_whenSendEmailVerification_shouldDisplayEmailVerificationSentScreen() throws Exception {
        when(identityService.isIdentityExistsForEmail(anyString())).thenReturn(false);
        when(identityService.isValidEmailDomain(anyString())).thenReturn(true);
        when(emailUpdateService.saveEmailUpdateAndNotify(isA(Identity.class), isA(String.class))).thenReturn(true);

        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", NEW_EMAIL)
                    .param("confirm", NEW_EMAIL)
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email update requested")))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andExpect(view().name(EMAIL_VERIFICATION_SENT_TEMPLATE));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq(NEW_EMAIL));
        verify(identityService, times(1)).isValidEmailDomain(eq(NEW_EMAIL));
        verify(emailUpdateService, times(1)).saveEmailUpdateAndNotify(isA(Identity.class), isA(String.class));
    }

    @Test
    public void givenAValidPendingEmailVerificationExist_shouldDisplayPendingEmailUpdateScreen() throws Exception {
        when(identityService.isIdentityExistsForEmail(anyString())).thenReturn(false);
        when(identityService.isValidEmailDomain(anyString())).thenReturn(true);
        when(emailUpdateService.saveEmailUpdateAndNotify(isA(Identity.class), isA(String.class))).thenReturn(false);

        mockMvc.perform(post(EMAIL_PATH)
                        .param("email", NEW_EMAIL)
                        .param("confirm", NEW_EMAIL)
                        .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email update pending")))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andExpect(view().name(PENDING_EMAIL_UPDATE_TEMPLATE));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq(NEW_EMAIL));
        verify(identityService, times(1)).isValidEmailDomain(eq(NEW_EMAIL));
        verify(emailUpdateService, times(1)).saveEmailUpdateAndNotify(isA(Identity.class), isA(String.class));
    }
}
