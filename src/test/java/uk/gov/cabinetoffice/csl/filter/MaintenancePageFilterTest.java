package uk.gov.cabinetoffice.csl.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.util.MaintenancePageUtil;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class MaintenancePageFilterTest {

    @Autowired
    private MaintenancePageFilter maintenancePageFilter;

    @MockBean
    private MaintenancePageUtil maintenancePageUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain mockFilterChain;

    @Test
    public void shouldNotRedirectToMaintenancePageWhenSkipMaintenancePageForUserIsTrue() throws ServletException, IOException {
        when(maintenancePageUtil.skipMaintenancePageForUser(request)).thenReturn(true);
        maintenancePageFilter.doFilterInternal(request, response, mockFilterChain);
        verify(response, times(0)).sendRedirect("/maintenance");
        maintenancePageFilter.destroy();
    }

    @Test
    public void shouldRedirectToMaintenancePageWhenSkipMaintenancePageForUserIsFalse() throws ServletException, IOException {
        when(maintenancePageUtil.skipMaintenancePageForUser(request)).thenReturn(false);
        maintenancePageFilter.doFilterInternal(request, response, mockFilterChain);
        verify(response, times(1)).sendRedirect("/maintenance");
        maintenancePageFilter.destroy();
    }

    @Test
    public void shouldNotFilterWhenShouldNotApplyFilterForURIIsTrue() throws ServletException, IOException {
        when(maintenancePageUtil.shouldNotApplyMaintenancePageFilterForURI(request)).thenReturn(true);
        assertTrue(maintenancePageFilter.shouldNotFilter(request));
        maintenancePageFilter.destroy();
    }

    @Test
    public void shouldFilterWhenShouldNotApplyFilterForURIIsFalse() throws ServletException, IOException {
        when(maintenancePageUtil.shouldNotApplyMaintenancePageFilterForURI(request)).thenReturn(false);
        assertFalse(maintenancePageFilter.shouldNotFilter(request));
        maintenancePageFilter.destroy();
    }
}
