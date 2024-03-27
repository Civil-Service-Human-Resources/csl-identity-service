package uk.gov.cabinetoffice.csl.controller.account.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.service.*;
import uk.gov.cabinetoffice.csl.util.ApplicationConstants;
import uk.gov.cabinetoffice.csl.util.WithMockCustomUser;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.STATUS_ATTRIBUTE;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockCustomUser
public class EmailUpdateControllerTest {

    private static final String UPDATE_EMAIL_FORM_TEMPLATE = "updateEmailForm";
    private static final String UPDATE_EMAIL_VIEW_NAME_TEMPLATE = "account/updateEmail";
    private static final String EMAIL_PATH = "/account/email";
    private static final String VERIFY_EMAIL_PATH = "/account/email/verify/";
    private static final String VERIFY_EMAIL_AGENCY_PATH = "/account/verify/agency/";
    private static final String VERIFY_CODE = "ZBnX9unEnnOcgMmCJ6rI3H2LUQFs2xsiMNj2Ejou";
    private static final String DOMAIN = "example.com";

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
                    .param("email", "basic@domain.com")
                    .param("confirm", "basic@domain.com")
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/email?emailAlreadyTaken=true"));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq("basic@domain.com"));
        verify(identityService, never()).isValidEmailDomain(anyString());
    }

    @Test
    public void givenAValidFormAndAnEmailThatIsNotValid_whenSendEmailVerification_shouldDisplayUnableToUseThisServiceError() throws Exception {
        when(identityService.isValidEmailDomain(anyString())).thenReturn(false);
        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", "basic@domain.com")
                    .param("confirm", "basic@domain.com")
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/email?notValidEmailDomain=true"));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq("basic@domain.com"));
        verify(identityService, times(1)).isValidEmailDomain(eq("basic@domain.com"));
    }

    @Test
    public void givenAValidFormAndEmailDoesNotAlreadyExistAndIsAValidEmail_whenSendEmailVerification_shouldDisplayEmailVerificationSentScreen() throws Exception {
        when(identityService.isIdentityExistsForEmail(anyString())).thenReturn(false);
        when(identityService.isValidEmailDomain(anyString())).thenReturn(true);
        mockMvc.perform(post(EMAIL_PATH)
                    .param("email", "basic@domain.com")
                    .param("confirm", "basic@domain.com")
                    .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/emailVerificationSent"));

        verify(identityService, times(1)).isIdentityExistsForEmail(eq("basic@domain.com"));
        verify(identityService, times(1)).isValidEmailDomain(eq("basic@domain.com"));
    }

    @Test
    public void shouldRedirectToErrorOccurredIfNewEmailIsNotAllowListedAndNotAgency() throws Exception {
        EmailUpdate emailUpdate = new EmailUpdate();
        emailUpdate.setCode(VERIFY_CODE);
        emailUpdate.setNewEmail("basic@domain.com");

        when(emailUpdateService.isEmailUpdateRequestExistsForCode(VERIFY_CODE)).thenReturn(true);
        when(emailUpdateService.getEmailUpdateRequestForCode(VERIFY_CODE)).thenReturn(emailUpdate);

        when(identityService.isAllowListedDomain(DOMAIN)).thenReturn(false);
        when(identityService.isDomainInAgency(DOMAIN)).thenReturn(false);

        mockMvc.perform(get(VERIFY_EMAIL_PATH + VERIFY_CODE)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(STATUS_ATTRIBUTE, ApplicationConstants.CHANGE_EMAIL_ERROR_MESSAGE))
                .andExpect(redirectedUrl("/login"))
                .andDo(print());
    }

    @Test
    public void givenAInvalidCode_whenUpdateEmail_shouldRedirectToUpdateEmailPageWithAnInvalidCodeError() throws Exception {
        when(emailUpdateService.isEmailUpdateRequestExistsForCode(anyString())).thenReturn(false);

        mockMvc.perform(get(VERIFY_EMAIL_PATH + VERIFY_CODE)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/email?invalidCode=true"))
                .andDo(print());

        verify(emailUpdateService, never()).updateEmailAddress(any(EmailUpdate.class));
    }

//    @Test
//    public void shouldRedirectToEmailUpdateIfNewEmailIsAllowListedButNotAgency() throws Exception {
//        EmailUpdate emailUpdate = new EmailUpdate();
//        emailUpdate.setCode(VERIFY_CODE);
//        emailUpdate.setNewEmail("basic@domain.com");
//
//        when(emailUpdateService.isEmailUpdateRequestExistsForCode(VERIFY_CODE)).thenReturn(true);
//        when(emailUpdateService.getEmailUpdateRequestForCode(VERIFY_CODE)).thenReturn(emailUpdate);
//
//        when(identityService.isAllowListedDomain(DOMAIN)).thenReturn(true);
//        when(identityService.isDomainInAgency(DOMAIN)).thenReturn(false);
//
//        doNothing().when(emailUpdateService).updateEmailAddress(eq(emailUpdate));
//
//        mockMvc.perform(get(VERIFY_EMAIL_PATH + VERIFY_CODE)
//                    .with(csrf())
//                )
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/account/email/updated"))
//                .andDo(print());
//    }

//    @Test
//    public void shouldRedirectToEmailUpdateIfNewEmailIsNotAllowListedButIsAgency() throws Exception {
//        EmailUpdate emailUpdate = new EmailUpdate();
//        emailUpdate.setCode(VERIFY_CODE);
//        emailUpdate.setNewEmail("basic@domain.com");
//
//        when(emailUpdateService.isEmailUpdateRequestExistsForCode(VERIFY_CODE)).thenReturn(true);
//        when(emailUpdateService.getEmailUpdateRequestForCode(VERIFY_CODE)).thenReturn(emailUpdate);
//
//        when(identityService.isAllowListedDomain(DOMAIN)).thenReturn(false);
//        when(identityService.isDomainInAgency(DOMAIN)).thenReturn(true);
//
//        doNothing().when(emailUpdateService).updateEmailAddress(eq(emailUpdate));
//
//        mockMvc.perform(get(VERIFY_EMAIL_PATH + VERIFY_CODE)
//                    .with(csrf())
//                )
//                .andExpect(status().is3xxRedirection())
//                .andExpect(flash().attribute("email", "basic@domain.com"))
//                .andExpect(redirectedUrl(VERIFY_EMAIL_AGENCY_PATH + VERIFY_CODE))
//                .andDo(print());
//    }



//    @Test
//    public void givenAValidCodeAndNonExistentIdentity_whenUpdateEmail_shouldRedirectToUpdateEmailPageWithAnInvalidEmailError() throws Exception {
//        EmailUpdate emailUpdate = new EmailUpdate();
//        emailUpdate.setCode(VERIFY_CODE);
//        emailUpdate.setNewEmail("basic@domain.com");
//
//        when(emailUpdateService.isEmailUpdateRequestExistsForCode(VERIFY_CODE)).thenReturn(true);
//        when(emailUpdateService.getEmailUpdateRequestForCode(VERIFY_CODE)).thenReturn(emailUpdate);
//        when(identityService.isAllowListedDomain(DOMAIN)).thenReturn(true);
//        when(identityService.isDomainInAgency(DOMAIN)).thenReturn(false);
//
//        doThrow(new ResourceNotFoundException("Not found")).when(emailUpdateService).updateEmailAddress(any(EmailUpdate.class));
//
//        mockMvc.perform(get(VERIFY_EMAIL_PATH + VERIFY_CODE)
//                    .with(csrf())
//                )
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/account/email?invalidEmail=true"))
//                .andDo(print());
//
//        verify(emailUpdateService, times(1)).updateEmailAddress(eq(emailUpdate));
//    }

//    @Test
//    public void givenAValidCodeAndATechnicalErrorWhenUpdating_whenUpdateEmail_shouldRedirectToUpdateEmailPageWithAnErrorOccurredError() throws Exception {
//        EmailUpdate emailUpdate = new EmailUpdate();
//        emailUpdate.setCode(VERIFY_CODE);
//        emailUpdate.setNewEmail("basic@domain.com");
//
//        when(emailUpdateService.isEmailUpdateRequestExistsForCode(VERIFY_CODE)).thenReturn(true);
//        when(emailUpdateService.getEmailUpdateRequestForCode(VERIFY_CODE)).thenReturn(emailUpdate);
//
//        when(identityService.isAllowListedDomain(DOMAIN)).thenReturn(true);
//        when(identityService.isDomainInAgency(DOMAIN)).thenReturn(false);
//
//        doThrow(new RuntimeException()).when(emailUpdateService).updateEmailAddress(any(EmailUpdate.class));
//
//        mockMvc.perform(get(VERIFY_EMAIL_PATH + VERIFY_CODE)
//                    .with(csrf())
//                )
//                .andExpect(status().is3xxRedirection())
//                .andExpect(flash().attribute(STATUS_ATTRIBUTE, ApplicationConstants.CHANGE_EMAIL_ERROR_MESSAGE))
//                .andExpect(redirectedUrl("/login"))
//                .andDo(print());
//
//        verify(emailUpdateService, times(1)).updateEmailAddress(eq(emailUpdate));
//    }

    @Test
    public void givenASuccessfulUpdateOfEmailAddress_whenEmailUpdated_shouldRedirectToEmailUpdatedView() throws Exception {
        mockMvc.perform(get("/account/email/updated")
                    .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("account/emailUpdated"));
    }
}
