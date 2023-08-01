package uk.gov.cabinetoffice.csl.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import uk.gov.cabinetoffice.csl.handler.CustomAuthenticationFailureHandler;
import uk.gov.cabinetoffice.csl.handler.WebSecurityExpressionHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.*;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_JWT;

@Configuration
public class SecurityConfig {

	private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

	@Value("${lpg.uiUrl}")
	private String lpgUiUrl;

	@Value("${management.endpoints.web.base-path}")
	private String actuatorBasePath;

	@Value("${oauth2.jwtKey}")
	private String jwtKey;

	@Value("${oauth2.accessTokenTTLSeconds}")
	private Long accessTokenTTLSeconds;

	@Value("${oauth2.refreshTokenTTLSeconds}")
	private Long refreshTokenTTLSeconds;

	@Value("${oauth2.scope}")
	private String accessTokenScope;

	@Value("${oauth2.requireAuthorizationConsent}")
	private Boolean requireAuthorizationConsent;

	@Value("${oauth2.requirePKCE}")
	private Boolean requirePKCE;

	//TODO: Below test properties will be removed after database implementation
	@Value("${test.redirectUri}")
	private String testRedirectUri;
	@Value("${test.client_id}")
	private String testClientId;
	@Value("${test.client_secret}")
	private String testClientSecret;
	@Value("${test.learner_user_id}")
	private String learnerUserId;
	@Value("${test.learner_user_password}")
	private String learnerUserPassword;
	@Value("${test.admin_user_id}")
	private String adminUserId;
	@Value("${test.admin_user_password}")
	private String adminUserPassword;
	@Value("${test.admin_user_roles}")
	private String adminUserRoles;

	public SecurityConfig(CustomAuthenticationFailureHandler customAuthenticationFailureHandler){
		this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain asSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(httpSecurity);
		httpSecurity.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(Customizer.withDefaults());
		httpSecurity
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.exceptionHandling(exceptions -> exceptions
				.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint("/login"),
					new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return httpSecurity.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain appSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(
						"/webjars/**",
						"/assets/**",
						"/css/**",
						"/img/**",
						"/favicon.ico",
						"/error",
						"/login",
						"/logout",
						actuatorBasePath + "/**").permitAll()
				.anyRequest().authenticated())
			.formLogin(formLogin -> formLogin
				.loginPage("/login").permitAll()
				.failureHandler(customAuthenticationFailureHandler)
				.defaultSuccessUrl(lpgUiUrl)
			)
			.logout(logout -> {
				logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
				logout.logoutSuccessHandler((request, response, authentication) -> {
					String redirectUrl = request.getParameter("returnTo");
					response.sendRedirect(Objects.requireNonNullElse(redirectUrl, "/login"));}
				);
			});
			//TODO: Below commented code will be removed if not used for future tickets.
			//.exceptionHandling(exceptions -> exceptions
			//		.defaultAuthenticationEntryPointFor(
			//				new LoginUrlAuthenticationEntryPoint("/login"),
			//				new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
		return httpSecurity.build();
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web
				.expressionHandler(new WebSecurityExpressionHandler());
				//TODO: Below commented code will be removed if not used for future tickets.
				//.ignoring()
				//.requestMatchers("/webjars/**","/assets/**","/css/**","/img/**","/favicon.ico");
	}

	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		RegisteredClient registeredClient =
			RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(testClientId)
				.clientSecret(passwordEncoder().encode(testClientSecret))
				.redirectUri(testRedirectUri)
				.scopes(scopes -> {
					scopes.add("read");
					scopes.add("write");})
				.clientAuthenticationMethods(methods -> {
					methods.add(CLIENT_SECRET_BASIC);
					methods.add(CLIENT_SECRET_JWT);})
				.authorizationGrantTypes(grantTypes -> {
					grantTypes.add(CLIENT_CREDENTIALS);
					grantTypes.add(AUTHORIZATION_CODE);
					grantTypes.add(REFRESH_TOKEN);
					grantTypes.add(JWT_BEARER);
					grantTypes.add(PASSWORD);})
				.clientSettings(clientSettings())
				.tokenSettings(tokenSettings())
				.build();
		return new InMemoryRegisteredClientRepository(registeredClient);
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
		return context -> {
			if(OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				context.getJwsHeader().algorithm(MacAlgorithm.HS256);
				context.getJwsHeader().type("JWT");
				String clientId = context.getRegisteredClient().getClientId();
				context.getClaims().claim("client_id", clientId);
				context.getClaims().claim(JwtClaimNames.JTI, UUID.randomUUID().toString());
				Set<String> scopes = new HashSet<>(Arrays.asList(accessTokenScope.split("\\s*,\\s*")));
				context.getClaims().claim("scope", scopes);
				context.getClaims().claim("scopes", scopes);
				Authentication principal = context.getPrincipal();
				Set<String> authorities = new HashSet<>();
				if (principal instanceof UsernamePasswordAuthenticationToken) {
					context.getClaims().claim("user_name", principal.getName());
					authorities = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
							.collect(Collectors.toSet());
				} else if (principal instanceof OAuth2ClientAuthenticationToken) {
					authorities.add("CLIENT");
				}
				context.getClaims().claim("authorities", authorities);
				//Audience is set to null to make the access token backward compatible with all the existing backend services
				context.getClaims().audience(new ArrayList<>());
			}
		};
	}

	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		SecretKey secretKey = new SecretKeySpec(jwtKey.getBytes(), "HMACSHA256");
		JWK jwk = new OctetSequenceKey.Builder(secretKey).algorithm(JWSAlgorithm.HS256).build();
		JWKSet jwkSet = new JWKSet(jwk);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(jwkSource());
	}

	@Bean
	TokenSettings tokenSettings() {
		return TokenSettings.builder()
				.accessTokenTimeToLive(Duration.ofSeconds(accessTokenTTLSeconds))
				.refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenTTLSeconds))
				.build();
	}

	@Bean
	ClientSettings clientSettings() {
		return ClientSettings.builder()
				.requireAuthorizationConsent(requireAuthorizationConsent)
				.requireProofKey(requirePKCE)
				.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		var learnerUser = User.withUsername(learnerUserId)
				.password(passwordEncoder().encode(learnerUserPassword))
				.authorities("LEARNER")
				.build();
		var superUser = User.withUsername(adminUserId)
				.password(passwordEncoder().encode(adminUserPassword))
				.authorities(adminUserRoles.split("\\s*,\\s*"))
				.build();
		return new InMemoryUserDetailsManager(learnerUser, superUser);
	}
}
