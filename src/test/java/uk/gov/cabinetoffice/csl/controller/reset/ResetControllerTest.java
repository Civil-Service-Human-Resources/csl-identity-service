package uk.gov.cabinetoffice.csl.controller.reset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.service.FrontendService;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.service.ResetService;
import uk.gov.cabinetoffice.csl.util.TestUtil;

import java.time.LocalDateTime;

import static java.time.Month.FEBRUARY;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.PENDING;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.RESET;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CONTACT_EMAIL_ATTRIBUTE;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.CONTACT_NUMBER_ATTRIBUTE;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class ResetControllerTest {

    private static final Long ID = 123L;
    private static final String UID = "uid123";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "Password123";
    private static final String CODE = "abc123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResetService resetService;

    @MockBean
    private FrontendService frontendService;

    @MockBean
    private IdentityService identityService;

    @Test
    public void shouldLoadRequestResetForm() throws Exception {
        mockMvc.perform(
                    get("/reset")
                    .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/requestReset"))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("Reset your password")))
                .andExpect(content().string(containsString("Enter your email")))
                .andExpect(content().string(containsString("Email address")))
                .andDo(print());
    }

    @Test
    public void shouldLoadCheckEmailPageIfIdentityExistsForTheGivenEmailId() throws Exception {
        when(identityService.isIdentityExistsForEmail(EMAIL)).thenReturn(true);
        mockMvc.perform(post("/reset")
                        .param("email", EMAIL)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/checkEmail"))
                .andExpect(content().string(containsString("Check your email")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("If <b><span>" + EMAIL + "</span></b> belongs to a valid account you will receive an email with a reset link.")))
                .andExpect(content().string(containsString("Please check your emails (including the junk/spam folder).")))
                .andExpect(model().attributeExists("resetEmailId"))
                .andExpect(content().string(containsString(EMAIL)))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());
    }

    @Test
    public void shouldLoadPendingResetPageIfUserTryToResetAgainWhilePendingResetExistsForTheGivenEmailId() throws Exception {
        when(identityService.isIdentityExistsForEmail(EMAIL)).thenReturn(true);
        Reset reset = createReset();
        LocalDateTime requestedAt = LocalDateTime.now();
        reset.setRequestedAt(requestedAt);

        when(resetService.getPendingResetForEmail(EMAIL)).thenReturn(reset);

        mockMvc.perform(post("/reset")
                        .param("email", EMAIL)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/pendingReset"))
                .andExpect(content().string(containsString("Password reset pending")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("Check your email for the link to reset your password.")))
                .andExpect(content().string(containsString("We recently sent you an email to reset your password.")))
                .andExpect(content().string(containsString("Please check your emails (including the junk/spam folder).")))
                .andExpect(content().string(containsString("Haven't received the email?")))
                .andExpect(content().string(containsString("Check your spam folder.")))
                .andExpect(content().string(containsString("If you don't see the email after 30 minutes, you can contact the Learning Platform")))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());
    }

    @Test
    public void shouldLoadCheckEmailPageIfIdentityDoesNotExistForTheGivenEmailId() throws Exception {
        when(identityService.isIdentityExistsForEmail(EMAIL)).thenReturn(false);
        mockMvc.perform(post("/reset")
                        .param("email", EMAIL)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/checkEmail"))
                .andExpect(content().string(containsString("Check your email")))
                .andExpect(content().string(containsString("What next?")))
                .andExpect(content().string(containsString("If <b><span>" + EMAIL + "</span></b> belongs to a valid account you will receive an email with a reset link.")))
                .andExpect(content().string(containsString("Please check your emails (including the junk/spam folder).")))
                .andExpect(model().attributeExists("resetEmailId"))
                .andExpect(content().string(containsString(EMAIL)))
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andDo(print());
    }

    @Test
    public void shouldLoadRequestResetFormWithErrorMessageIfResetCodeDoesNotExist() throws Exception {
        when(resetService.getResetForCode(CODE)).thenReturn(null);
        mockMvc.perform(
                        get("/reset/" + CODE)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/requestReset"))
                .andExpect(content().string(containsString("The reset link is invalid.\nPlease re-submit the reset request.")))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("Reset your password")))
                .andExpect(content().string(containsString("Enter your email")))
                .andExpect(content().string(containsString("Email address")))
                .andDo(print());
    }

    @Test
    public void shouldLoadRequestResetFormWithErrorMessageIfResetCodeIsExpired() throws Exception {
        Reset reset = createReset();
        LocalDateTime requestDateTime = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        reset.setRequestedAt(requestDateTime);

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(true);

        mockMvc.perform(
                        get("/reset/" + CODE)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/requestReset"))
                .andExpect(content().string(containsString("The reset link is expired.\nPlease re-submit the reset request.")))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("Reset your password")))
                .andExpect(content().string(containsString("Enter your email")))
                .andExpect(content().string(containsString("Email address")))
                .andDo(print());
    }

    @Test
    public void shouldLoadRequestResetFormWithErrorMessageIfResetCodeIsAlreadyUsed() throws Exception {
        Reset reset = createReset();
        reset.setResetStatus(RESET);
        LocalDateTime requestDateTime = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        reset.setRequestedAt(requestDateTime);

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);
        when(resetService.isResetComplete(reset)).thenReturn(true);

        mockMvc.perform(
                        get("/reset/" + CODE)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/requestReset"))
                .andExpect(content().string(containsString("The reset link is already used.\nPlease re-submit the reset request.")))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("Reset your password")))
                .andExpect(content().string(containsString("Enter your email")))
                .andExpect(content().string(containsString("Email address")))
                .andDo(print());
    }

    @Test
    public void shouldLoadPasswordFormIfCodeIsPending() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        mockMvc.perform(
                        get("/reset/" + CODE)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordForm"))
                .andExpect(content().string(containsString("Create a memorable password for your account.")))
                .andExpect(content().string(containsString("Your password must have:")))
                .andExpect(content().string(containsString("8 or more characters")))
                .andExpect(content().string(containsString("at least 1 number")))
                .andExpect(content().string(containsString("upper and lower case letters")))
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("Re-type your Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("id=\"passwordConfirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldShowPasswordInvalidErrorMessageIfNumberIsMissingFromPassword() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Password")
                        .param("confirmPassword", "Password")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordForm"))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("Password is invalid")))
                .andExpect(content().string(containsString("Create a memorable password for your account.")))
                .andExpect(content().string(containsString("Your password must have:")))
                .andExpect(content().string(containsString("8 or more characters")))
                .andExpect(content().string(containsString("at least 1 number")))
                .andExpect(content().string(containsString("upper and lower case letters")))
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("Re-type your Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("id=\"passwordConfirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldShowPasswordInvalidErrorMessageIfLowerCaseCharacterMissingFromPassword() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "PASSWORD123")
                        .param("confirmPassword", "PASSWORD123")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordForm"))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("Password is invalid")))
                .andExpect(content().string(containsString("Create a memorable password for your account.")))
                .andExpect(content().string(containsString("Your password must have:")))
                .andExpect(content().string(containsString("8 or more characters")))
                .andExpect(content().string(containsString("at least 1 number")))
                .andExpect(content().string(containsString("upper and lower case letters")))
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("Re-type your Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("id=\"passwordConfirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldShowPasswordInvalidErrorMessageIfUpperCaseCharacterMissingFromPassword() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordForm"))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("Password is invalid")))
                .andExpect(content().string(containsString("Create a memorable password for your account.")))
                .andExpect(content().string(containsString("Your password must have:")))
                .andExpect(content().string(containsString("8 or more characters")))
                .andExpect(content().string(containsString("at least 1 number")))
                .andExpect(content().string(containsString("upper and lower case letters")))
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("Re-type your Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("id=\"passwordConfirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldShowPasswordInvalidErrorMessageIfPasswordLengthIsLessThan8Characters() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Pass123")
                        .param("confirmPassword", "Pass123")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordForm"))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("Password is invalid")))
                .andExpect(content().string(containsString("Create a memorable password for your account.")))
                .andExpect(content().string(containsString("Your password must have:")))
                .andExpect(content().string(containsString("8 or more characters")))
                .andExpect(content().string(containsString("at least 1 number")))
                .andExpect(content().string(containsString("upper and lower case letters")))
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("Re-type your Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("id=\"passwordConfirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldShowPasswordMismatchErrorMessageIfPasswordAndConfirmPasswordMismatch() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Password123")
                        .param("confirmPassword", "PasswordMisMatch")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordForm"))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("Passwords do not match")))
                .andExpect(content().string(containsString("Create a memorable password for your account.")))
                .andExpect(content().string(containsString("Your password must have:")))
                .andExpect(content().string(containsString("8 or more characters")))
                .andExpect(content().string(containsString("at least 1 number")))
                .andExpect(content().string(containsString("upper and lower case letters")))
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("Re-type your Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("id=\"passwordConfirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldLoadRequestResetFormWithErrorMessageIfIdentityDoesNotExist() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);
        when(resetService.isResetComplete(reset)).thenReturn(false);
        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(null);

        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/requestReset"))
                .andExpect(content().string(containsString("The reset link is invalid.\nPlease submit the reset request for the valid email id.")))
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(content().string(containsString("Reset your password")))
                .andExpect(content().string(containsString("Enter your email")))
                .andExpect(content().string(containsString("Email address")))
                .andDo(print());
    }

    @Test
    public void shouldLoadPasswordResetSuccessful() throws Exception {
        Reset reset = createReset();

        when(resetService.getResetForCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);

        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(identityService.getIdentityForEmail(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("reset/passwordReset"))
                .andExpect(content().string(containsString("Password reset complete")))
                .andExpect(content().string(containsString("Your new password has been changed")))
                .andExpect(content().string(containsString("What happens next?")))
                .andExpect(model().attributeExists("lpgUiUrl"))
                .andDo(print());

        verify(frontendService, atMostOnce()).signoutUser(UID);
    }

    private Reset createReset() {
        Reset reset = new Reset();
        reset.setCode(CODE);
        reset.setEmail(EMAIL);
        reset.setResetStatus(PENDING);
        LocalDateTime requestDateTime = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        reset.setRequestedAt(requestDateTime);
        return reset;
    }
}
