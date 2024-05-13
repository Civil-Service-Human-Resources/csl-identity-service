package uk.gov.cabinetoffice.csl.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@WebFilter
@Component
@Order(3)
public class MaintenancePageFilter extends OncePerRequestFilter {

	private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

	private final boolean maintenancePageEnabled;

	private final String skipMaintenancePageForUsers;

	public MaintenancePageFilter(@Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
								 @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers) {
		this.maintenancePageEnabled = maintenancePageEnabled;
		this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
		log.info("MaintenancePageFilter.doFilterInternal: username request param: {}", username);
		String method = request.getMethod();
		log.info("MaintenancePageFilter.doFilterInternal: method: {}", method);

		if(maintenancePageEnabled) {
			boolean skipMaintenancePage = isNotBlank(username) &&
					Arrays.stream(skipMaintenancePageForUsers.split(","))
							.anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
			if(skipMaintenancePage) {
				log.info("MaintenancePageFilter.doFilterInternal: Maintenance page is skipped for the user: {}", username);
			} else {
				response.sendRedirect("/maintenance");
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		log.info("MaintenancePageFilter.shouldNotFilter: servletPath: {}", request.getServletPath());
		String requestURI = request.getRequestURI();
		log.info("MaintenancePageFilter.shouldNotFilter: requestURI: {}", requestURI);
		return "/health".equalsIgnoreCase(requestURI)
				|| "/info".equalsIgnoreCase(requestURI)
				|| "/maintenance".equalsIgnoreCase(requestURI);
	}
}
