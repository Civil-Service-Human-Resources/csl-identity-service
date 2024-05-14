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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockCustomUser
public class MaintenancePageControllerTest {

    private static final String MAINTENANCE_TEMPLATE = "maintenance/maintenance";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldDisplayMaintenancePage() throws Exception {
        mockMvc.perform(get("/maintenance")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name(MAINTENANCE_TEMPLATE))
                .andExpect(content().string(containsString("Maintenance")))
                .andExpect(content().string(containsString("The learning website is undergoing scheduled maintenance.")))
                .andExpect(content().string(containsString("Apologies for the inconvenience.")))
                .andDo(print());
    }
}
