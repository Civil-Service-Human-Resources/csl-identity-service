package uk.gov.cabinetoffice.csl.controller.account.agencytoken;

import com.nimbusds.jose.shaded.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.cabinetoffice.csl.dto.AgencyTokenCapacityUsedDTO;
import uk.gov.cabinetoffice.csl.service.AgencyTokenCapacityService;
import uk.gov.cabinetoffice.csl.util.WithMockCustomUser;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockCustomUser
public class AgencyControllerTest {

    private static final String UID = "UID";

    private final Gson gson = new Gson();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgencyTokenCapacityService agencyTokenCapacityService;

    @Test
    public void getSpacesUsedForAgencyToken() throws Exception {
        AgencyTokenCapacityUsedDTO agencyTokenCapacityUsedDto = new AgencyTokenCapacityUsedDTO(100L);

        when(agencyTokenCapacityService.getSpacesUsedByAgencyToken(UID)).thenReturn(agencyTokenCapacityUsedDto);

        mockMvc.perform(
                MockMvcRequestBuilders.get(String.format("/agency/%s", UID))
                        .accept(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(gson.toJson(agencyTokenCapacityUsedDto)));
    }

    @Test
    public void deleteAgencyToken_callsAgencyTokenCapacityServiceDeleteAgencyTokenOk() throws Exception {
        String agencyTokenUid = UUID.randomUUID().toString();
        mockMvc.perform(
                MockMvcRequestBuilders.delete(String.format("/agency/%s", agencyTokenUid))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(agencyTokenCapacityService, times(1)).deleteAgencyToken(agencyTokenUid);
    }

//    @Test
//    public void deleteAgencyToken_callsAgencyTokenCapacityServiceDeleteAgencyTokenError() throws Exception {
//        String agencyTokenUid = UUID.randomUUID().toString();
//
//        doThrow(Exception.class).when(agencyTokenCapacityService).deleteAgencyToken(agencyTokenUid);
//
//        mockMvc.perform(
//                MockMvcRequestBuilders.delete(String.format("/agency/%s", agencyTokenUid))
//                        .with(csrf())
//        ).andExpect(status().is5xxServerError());
//
//        verify(agencyTokenCapacityService, times(1)).deleteAgencyToken(agencyTokenUid);
//    }
}
