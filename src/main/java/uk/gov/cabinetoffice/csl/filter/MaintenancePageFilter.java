package uk.gov.cabinetoffice.csl.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
@Order(3)
public class MaintenancePageFilter implements Filter {

	private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

	private final boolean maintenancePageEnabled;

	private final String skipMaintenancePageForUsers;

	public MaintenancePageFilter(@Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
								 @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers) {
		this.maintenancePageEnabled = maintenancePageEnabled;
		this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		log.debug("MaintenancePageFilter.doFilter.start");
		displayMaintenancePage(httpRequest, httpResponse);
		log.debug("MaintenancePageFilter.doFilter.end");
		chain.doFilter(request, response);
	}

	private void displayMaintenancePage(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
		log.info("MaintenancePageFilter.displayMaintenancePage: username request param: {}", username);
		if(maintenancePageEnabled) {
			boolean skipMaintenancePage = isNotBlank(username) &&
					Arrays.stream(skipMaintenancePageForUsers.split(","))
							.anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
			if (skipMaintenancePage) {
				log.info("MaintenancePageFilter.displayMaintenancePage: Maintenance page is skipped for the user: {}", username);
				return;
			}
			response.sendRedirect("/maintenance");
		}
	}
}
