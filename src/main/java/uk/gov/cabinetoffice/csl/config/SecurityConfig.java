package uk.gov.cabinetoffice.csl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.handler.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;
import static org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN;

@Configuration
public class SecurityConfig {

	@Value("${management.endpoints.web.base-path}")
	private String actuatorBasePath;

	private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
	private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
	private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
	private final CustomCookieAndAuth2TokenClearingLogoutHandler customCookieAndAuth2TokenClearingLogoutHandler;

	public SecurityConfig(CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
						  CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
						  CustomLogoutSuccessHandler customLogoutSuccessHandler,
						  CustomCookieAndAuth2TokenClearingLogoutHandler customCookieAndAuth2TokenClearingLogoutHandler){
		this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
		this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
		this.customLogoutSuccessHandler = customLogoutSuccessHandler;
		this.customCookieAndAuth2TokenClearingLogoutHandler = customCookieAndAuth2TokenClearingLogoutHandler;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain asSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(httpSecurity);
		httpSecurity.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(Customizer.withDefaults());
		httpSecurity
			.cors(Customizer.withDefaults())
			.csrf(Customizer.withDefaults())
			.exceptionHandling(exceptions -> exceptions
				.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint("/login"),
					new MediaTypeRequestMatcher(TEXT_HTML)))
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return httpSecurity.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain appSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
			.cors(Customizer.withDefaults())
			.csrf(Customizer.withDefaults())
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/webjars/**", "/assets/**", "/css/**", "/img/**", "/favicon.ico",
					"/cookies","/privacy","/accessibility-statement","/contact-us",
					"/health","/maintenance","/error",
					"/signup/**", "/login", "/reset/**",
					"/account/reactivate/**","/account/verify/agency/**",
					"/account/email/verify/**","/account/email/updated/**","/account/email/update/error/**",
					actuatorBasePath + "/**").permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(formLogin -> formLogin
				.loginPage("/login").permitAll()
				.failureHandler(customAuthenticationFailureHandler)
				.successHandler(customAuthenticationSuccessHandler)
			)
			.logout(logout -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.addLogoutHandler(customCookieAndAuth2TokenClearingLogoutHandler)
				.clearAuthentication(true)
				.invalidateHttpSession(true)
				.logoutSuccessHandler(customLogoutSuccessHandler)
			)
			.exceptionHandling(exceptions -> exceptions
				.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint("/login"),
					new MediaTypeRequestMatcher(TEXT_HTML))
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return httpSecurity.build();
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web
				.expressionHandler(new WebSecurityExpressionHandler());
	}

	@Bean
	public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcRegisteredClientRepository(jdbcTemplate);
	}

	@Bean
	public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
		return context -> {
			if(ACCESS_TOKEN.equals(context.getTokenType())) {
				context.getJwsHeader().algorithm(HS256);
				context.getJwsHeader().type("JWT");
				RegisteredClient client = context.getRegisteredClient();
				context.getClaims().claim("client_id", client.getClientId());
				context.getClaims().claim(JwtClaimNames.JTI, UUID.randomUUID().toString());
				context.getClaims().claim("scopes", client.getScopes());
				Authentication principal = context.getPrincipal();
				Set<String> authorities = new HashSet<>();
				if (principal instanceof UsernamePasswordAuthenticationToken) {
					context.getClaims().claim("user_name", principal.getName());
					authorities = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
							.collect(Collectors.toSet());
					if (principal.getPrincipal() instanceof IdentityDetails) {
						String email = ((IdentityDetails) principal.getPrincipal()).getIdentity().getEmail();
						context.getClaims().claim("email", email);
					}
				} else if (principal instanceof OAuth2ClientAuthenticationToken) {
					context.getClaims().claim("user_name", principal.getName());
					authorities.add("CLIENT");
				}
				context.getClaims().claim("authorities", authorities);
				//Audience is set to null to make the access token backward compatible with all the existing backend services
				context.getClaims().audience(new ArrayList<>());
			}
		};
	}
}
