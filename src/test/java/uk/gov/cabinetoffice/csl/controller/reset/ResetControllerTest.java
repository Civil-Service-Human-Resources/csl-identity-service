package uk.gov.cabinetoffice.csl.controller.reset;

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
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.repository.ResetRepository;
import uk.gov.cabinetoffice.csl.service.ResetService;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.PENDING;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class ResetControllerTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "abc123";
    private static final String UID = "uid123";
    private static final Boolean ACTIVE = true;
    private static final Boolean LOCKED = false;
    private static final String PASSWORD = "password";
    private static final Set<Role> ROLES = new HashSet<>();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResetService resetService;

    @MockBean
    private ResetRepository resetRepository;

    @MockBean
    private IdentityRepository identityRepository;

    @Test
    public void shouldLoadRequestResetForm() throws Exception {
        mockMvc.perform(
                    get("/reset")
                    .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"email\"")))
                .andExpect(MockMvcResultMatchers.view().name("reset/requestReset"))
                .andDo(print());
    }

    @Test
    public void shouldLoadCheckEmailIfUserNameExists() throws Exception {
        when(identityRepository.existsByEmail(EMAIL)).thenReturn(true);

        mockMvc.perform(post("/reset")
                        .param("email", EMAIL)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Check your email for the link to reset your password.")))
                .andExpect(content().string(containsString("Check your spam folder.")))
                .andExpect(content().string(containsString("If you don't see the email after 30 minutes, you can contact the Learning Platform")))
                .andExpect(MockMvcResultMatchers.view().name("reset/checkEmail"))
                .andDo(print());
    }

    @Test
    public void shouldLoadResetIfUserNameDoesNotExist() throws Exception {
        when(identityRepository.existsByEmail(EMAIL)).thenReturn(false);

        mockMvc.perform(post("/reset")
                        .param("email", EMAIL)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Check your email for the link to reset your password.")))
                .andExpect(content().string(containsString("Check your spam folder.")))
                .andExpect(content().string(containsString("If you don't see the email after 30 minutes, you can contact the Learning Platform")))
                .andExpect(MockMvcResultMatchers.view().name("reset/checkEmail"))
                .andDo(print());
    }

    @Test
    public void shouldReturnResourceNotFoundIfCodeNotFound() throws Exception {
        when(resetRepository.findByCode(CODE)).thenReturn(null);
        mockMvc.perform(
                        get("/reset/" + CODE)
                        .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    public void shouldLoadResetIfCodeIsExpired() throws Exception {
        Reset reset = new Reset();
        reset.setEmail(EMAIL);
        reset.setCode(CODE);
        reset.setRequestedAt(new Date(2323223232L));

        when(resetRepository.findByCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(true);

        mockMvc.perform(
                        get("/reset/" + CODE)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.view().name("redirect:/reset"))
                .andExpect(redirectedUrl("/reset"))
                .andDo(print());
    }

    @Test
    public void shouldLoadPasswordFormIfCodeIsPending() throws Exception {
        Reset reset = new Reset();
        reset.setEmail(EMAIL);
        reset.setCode(CODE);
        reset.setResetStatus(PENDING);
        reset.setRequestedAt(new Date());

        when(resetRepository.findByCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);
        when(resetService.isResetPending(reset)).thenReturn(true);

        mockMvc.perform(
                        get("/reset/" + CODE)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("reset/passwordForm"))
                .andDo(print());
    }

    @Test
    public void shouldLoadPasswordResetSuccessful() throws Exception {
        Reset reset = new Reset();
        reset.setEmail(EMAIL);
        reset.setCode(CODE);
        reset.setResetStatus(PENDING);
        reset.setRequestedAt(new Date());

        when(resetRepository.findByCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);
        when(resetService.isResetPending(reset)).thenReturn(true);

        Identity identity = new Identity(UID, EMAIL, PASSWORD, ACTIVE, LOCKED, ROLES,
                Instant.now(), false, "AgencyTokenUid");

        when(identityRepository.findFirstByEmailEquals(EMAIL)).thenReturn(identity);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("reset/passwordReset"))
                .andDo(print());
    }

    @Test
    public void shouldReturnResourceNotFoundIfIdentityNotFound() throws Exception {
        Reset reset = new Reset();
        reset.setEmail(EMAIL);
        reset.setCode(CODE);
        reset.setResetStatus(PENDING);
        reset.setRequestedAt(new Date());

        when(resetRepository.findByCode(CODE)).thenReturn(reset);
        when(resetService.isResetExpired(reset)).thenReturn(false);
        when(resetService.isResetPending(reset)).thenReturn(true);

        when(identityRepository.findFirstByEmailEquals(EMAIL)).thenReturn(null);
        mockMvc.perform(post("/reset/" + CODE)
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
