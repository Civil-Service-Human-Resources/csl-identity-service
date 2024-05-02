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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private static final String CONTACT_EMAIL_ATTRIBUTE = "contactEmail";
    private static final String CONTACT_NUMBER_ATTRIBUTE = "contactNumber";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldDisplayCookiesPage() throws Exception {
        mockMvc.perform(get(COOKIES_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(COOKIES_TEMPLATE))
                .andDo(print());
    }

    @Test
    public void shouldDisplayPrivacyPage() throws Exception {
        mockMvc.perform(get(PRIVACY_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(view().name(PRIVACY_TEMPLATE))
                .andDo(print());
    }

    @Test
    public void shouldDisplayContactUsPage() throws Exception {
        mockMvc.perform(get(CONTACT_US_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(view().name(CONTACT_US_TEMPLATE))
                .andDo(print());
    }

    @Test
    public void shouldDisplayAccessibilityStatementPage() throws Exception {
        mockMvc.perform(get(ACCESSIBILITY_STATEMENT_PATH)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(CONTACT_EMAIL_ATTRIBUTE))
                .andExpect(model().attributeExists(CONTACT_NUMBER_ATTRIBUTE))
                .andExpect(view().name(ACCESSIBILITY_STATEMENT_TEMPLATE))
                .andDo(print());
    }
}
