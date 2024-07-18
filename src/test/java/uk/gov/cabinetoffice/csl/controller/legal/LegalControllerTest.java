package uk.gov.cabinetoffice.csl.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cabinetoffice.csl.util.WithMockCustomUser;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.cabinetoffice.csl.util.ApplicationConstants.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockCustomUser
public class LegalControllerTest {

    private static final String COOKIES_PATH = "/cookies";
    private static final String PRIVACY_PATH = "/privacy";
    private static final String ACCESSIBILITY_STATEMENT_PATH = "/accessibility-statement";
    private static final String CONTACT_US_PATH = "/contact-us";

    private static final String COOKIES_TEMPLATE = "legal/cookies";
    private static final String PRIVACY_TEMPLATE = "legal/privacy";
    private static final String CONTACT_US_TEMPLATE = "legal/contact-us";
    private static final String ACCESSIBILITY_STATEMENT_TEMPLATE = "legal/accessibility-statement";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldDisplayCookiesPage() throws Exception {
        mockMvc.perform(get(COOKIES_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(COOKIES_TEMPLATE))
                .andExpect(content().string(containsString("Cookies")))
                .andDo(print());
    }

    @Test
    public void shouldDisplayPrivacyPage() throws Exception {
        mockMvc.perform(get(PRIVACY_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(view().name(PRIVACY_TEMPLATE))
                .andExpect(content().string(containsString("Privacy notice for Civil Service Learning")))
                .andDo(print());
    }

    @Test
    public void shouldDisplayContactUsPage() throws Exception {
        mockMvc.perform(get(CONTACT_US_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andExpect(view().name(CONTACT_US_TEMPLATE))
                .andExpect(content().string(containsString("Contact us")))
                .andDo(print());
    }

    @Test
    public void shouldDisplayAccessibilityStatementPage() throws Exception {
        mockMvc.perform(get(ACCESSIBILITY_STATEMENT_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(content().string(containsString("support@governmentcampus.co.uk")))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(content().string(containsString("020 3640 7985")))
                .andExpect(view().name(ACCESSIBILITY_STATEMENT_TEMPLATE))
                .andExpect(content().string(containsString("Accessibility statement")))
                .andDo(print());
    }
}
