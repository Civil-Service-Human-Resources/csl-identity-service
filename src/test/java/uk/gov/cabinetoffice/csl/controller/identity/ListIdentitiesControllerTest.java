package uk.gov.cabinetoffice.csl.controller.identity;

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
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.service.IdentityService;
import uk.gov.cabinetoffice.csl.util.TestUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("no-redis")
@WithMockUser(username = "user")
public class ListIdentitiesControllerTest {

    private static final String GET_IDENTITY_AGENCY_TOKEN_UID_URL = "/api/identity/agency/";
    private static final String IDENTITY_UID = "abc123";
    private static final String EMAIL = "test@example.org";
    private static final String PASSWORD = "password123";
    private static final String AGENCY_TOKEN_UID = "456";

    @MockBean
    private IdentityService identityService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnIdentityAgencyDTOSuccessfully() throws Exception {
        Identity identity = TestUtil.createIdentity(IDENTITY_UID, EMAIL, PASSWORD, AGENCY_TOKEN_UID);
        when(identityService.getIdentityForUid(eq(IDENTITY_UID))).thenReturn(identity);

        mockMvc.perform(
                get(GET_IDENTITY_AGENCY_TOKEN_UID_URL + IDENTITY_UID))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.uid", is(IDENTITY_UID)))
                .andExpect(jsonPath("$.agencyTokenUid", is(identity.getAgencyTokenUid())));
    }

    @Test
    public void shouldReturnNotFoundWhenIdentityNotFound() throws Exception {
        when(identityService.getIdentityForUid(eq(IDENTITY_UID)))
                .thenThrow(new IdentityNotFoundException("Identity not found for uid: " + IDENTITY_UID));

        mockMvc.perform(
                get(GET_IDENTITY_AGENCY_TOKEN_UID_URL + IDENTITY_UID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn500WhenTechnicalError() throws Exception {
        when(identityService.getIdentityForUid(eq(IDENTITY_UID))).thenThrow(new RuntimeException());

        mockMvc.perform(
                get(GET_IDENTITY_AGENCY_TOKEN_UID_URL + IDENTITY_UID))
                .andExpect(status().isInternalServerError());
    }
}


