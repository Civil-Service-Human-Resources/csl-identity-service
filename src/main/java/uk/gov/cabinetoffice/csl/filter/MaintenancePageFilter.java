package uk.gov.cabinetoffice.csl.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.cabinetoffice.csl.util.MaintenancePageUtil;

import java.io.IOException;

@AllArgsConstructor
@Component
@Order(3)
public class MaintenancePageFilter extends OncePerRequestFilter {

	private final MaintenancePageUtil maintenancePageUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		if(!maintenancePageUtil.skipMaintenancePageForUser(request)) {
			response.sendRedirect("/maintenance");
			return;
		}
		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return maintenancePageUtil.shouldNotApplyFilterForURI(request);
	}
}
