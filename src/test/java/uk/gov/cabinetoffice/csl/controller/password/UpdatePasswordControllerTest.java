package uk.gov.cabinetoffice.csl.controller.password;

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.service.UserService;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class UpdatePasswordControllerTest {

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "Password123";
    private static final String UID = "uid123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private IUserAuthService userAuthService;

    @Test
    public void shouldLoadPasswordResetForm() throws Exception {
        mockMvc.perform(
                    get("/account/password")
                    .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("account/updatePassword"))
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
    public void shouldLoadCheckEmailPageIfIdentityExistsForTheGivenEmailId() throws Exception {
        Identity identity = createIdentity();
        when(userAuthService.getIdentity()).thenReturn(identity);
        when(userService.checkPassword(identity.getEmail(), PASSWORD)).thenReturn(true);
        doNothing().when(userService).updatePasswordAndNotify(identity, PASSWORD);

        mockMvc.perform(post("/account/password")
                        .param("password", PASSWORD)
                        .param("newPassword", PASSWORD)
                        .param("confirm", "Password")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("account/updatePassword"))
                .andExpect(content().string(containsString("Change your password")))
                .andExpect(content().string(containsString("There was a problem with your password")))
                .andExpect(content().string(containsString("New password fields do not match")))
                .andDo(print());
    }

    private Identity createIdentity(){
        Identity identity = new Identity();
        identity.setPassword(PASSWORD);
        identity.setActive(true);
        identity.setLocked(false);
        identity.setId(1234L);
        identity.setEmail(EMAIL);
        identity.setUid(UID);
        return identity;
    }
}
