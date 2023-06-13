package uk.gov.cabinetoffice.csl.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
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
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class SecurityConfig {
	@Value("${oauth2.redirectUri}")
	String redirectUri;

	@Value("${oauth2.jwtKey}")
	private String jwtKey;

	@Bean
	@Order(1)
	public SecurityFilterChain asSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(Customizer.withDefaults());
		http
//			.cors(Customizer.withDefaults())
//			.csrf(AbstractHttpConfigurer::disable)
//			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling((exceptions) -> exceptions
				.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint("/login"),
					new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
				)
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
		http
//			.cors(Customizer.withDefaults())
//			.csrf(AbstractHttpConfigurer::disable)
//			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.formLogin(formLogin -> formLogin
				.loginPage("/login").permitAll()
			);
		return http.build();
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.debug(false)
				.ignoring()
				.requestMatchers("/webjars/**", "/images/**", "/css/**", "/assets/**", "/favicon.ico");
	}

	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		RegisteredClient registeredClient =
			RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId("client")
			.clientSecret(passwordEncoder().encode("secret"))
			.scope("read")
//			.scope(OidcScopes.OPENID)
//			.scope(OidcScopes.PROFILE)
			.redirectUri(redirectUri)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
			.authorizationGrantType(AuthorizationGrantType.PASSWORD)
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
			context.getJwsHeader().algorithm(MacAlgorithm.HS256);
			Authentication principal = context.getPrincipal();
			log.debug("principal: {}", principal);
			if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				Set<String> authorities = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
						.collect(Collectors.toSet());
				log.debug("authorities: {}", authorities);
				context.getClaims().claim("authorities", authorities);
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
				.accessTokenTimeToLive(Duration.ofMinutes(15))
				.refreshTokenTimeToLive(Duration.ofMinutes(20))
				.build();
	}

	@Bean
	ClientSettings clientSettings() {
		return ClientSettings.builder()
				.requireAuthorizationConsent(false)
				.requireProofKey(true)
				.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		var user1 = User.withUsername("user")
				.password(passwordEncoder().encode("password"))
				.authorities("read")
				.roles("USER")
				.build();
		var user2 = User.withUsername("admin")
				.password(passwordEncoder().encode("password"))
				.authorities("read","write")
				.roles("USER", "ADMIN")
				.build();
		return new InMemoryUserDetailsManager(user1, user2);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
