package uk.gov.cabinetoffice.csl.controller.maintenance;

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
public class MaintenancePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockCustomUser
    public void shouldDisplayMaintenancePageForLoggedInUser() throws Exception {
        String lpgUiUrl = "http://localhost:3001";
        mockMvc.perform(get("/maintenance")
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(lpgUiUrl))
                .andDo(print());
    }

    @Test
    public void shouldDisplayMaintenancePageForNotLoggedInUser() throws Exception {
        String lpgUiUrl = "http://localhost:3001";
        mockMvc.perform(get("/maintenance")
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(lpgUiUrl))
                .andDo(print());
    }
}
