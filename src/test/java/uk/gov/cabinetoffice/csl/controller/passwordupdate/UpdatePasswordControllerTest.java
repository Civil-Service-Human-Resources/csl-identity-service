package uk.gov.cabinetoffice.csl.controller.passwordupdate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.service.FrontendService;
import uk.gov.cabinetoffice.csl.service.PasswordService;
import uk.gov.cabinetoffice.csl.util.TestUtil;
import uk.gov.cabinetoffice.csl.util.WithMockCustomUser;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockCustomUser
public class UpdatePasswordControllerTest {

    private static final String UPDATE_PASSWORD_TEMPLATE = "passwordupdate/updatePassword";
    private static final String PASSWORD_UPDATED_TEMPLATE = "passwordupdate/passwordUpdated";
    private static final String REDIRECT_PASSWORD_UPDATED = "/account/password/passwordUpdated";

    private static final Long ID = 123L;
    private static final String UID = "uid123";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FrontendService frontendService;

    @MockBean
    private PasswordService passwordService;

    @Test
    public void shouldLoadPasswordResetForm() throws Exception {
        mockMvc.perform(
                    get("/account/password")
                    .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(UPDATE_PASSWORD_TEMPLATE))
                .andExpect(content().string(containsString("Change your password")))
                .andExpect(content().string(containsString("Current Password")))
                .andExpect(content().string(containsString("id=\"password\"")))
                .andExpect(content().string(containsString("New Password")))
                .andExpect(content().string(containsString("id=\"new-password\"")))
                .andExpect(content().string(containsString("Confirm New Password")))
                .andExpect(content().string(containsString("id=\"confirm\"")))
                .andDo(print());
    }

    @Test
    public void shouldLoadPasswordResetFormWithCurrentPasswordIncorrectError() throws Exception {
        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);
        when(passwordService.isPasswordMatches(identity.getEmail(), "currentPassword123")).thenReturn(false);

        mockMvc.perform(post("/account/password")
                        .param("password", "currentPassword123")
                        .param("newPassword", "Password1234")
                        .param("confirm", "Password1234")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(UPDATE_PASSWORD_TEMPLATE))
                .andExpect(content().string(containsString("Change your password")))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("Current password is incorrect")))
                .andDo(print());
    }

    @Test
    public void shouldUpdatePassword() throws Exception {
        Identity identity = TestUtil.createIdentity(ID, UID, EMAIL, PASSWORD, null);

        when(passwordService.isPasswordMatches(identity.getEmail(), PASSWORD)).thenReturn(true);
        doNothing().when(passwordService).updatePasswordAndNotify(identity, PASSWORD);

        mockMvc.perform(post("/account/password")
                        .param("password", PASSWORD)
                        .param("newPassword", PASSWORD)
                        .param("confirm", PASSWORD)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(PASSWORD_UPDATED_TEMPLATE))
                .andExpect(content().string(containsString("Your password has been updated")))
                .andExpect(model().attributeExists("lpgUiUrl"))
                .andDo(print());

        verify(frontendService, times(1)).signoutUser(UID);
    }

}
