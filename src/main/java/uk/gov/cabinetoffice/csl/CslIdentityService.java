package uk.gov.cabinetoffice.csl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import uk.gov.service.notify.NotificationClient;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CslIdentityService {

	public static void main(String[] args) {
		SpringApplication.run(CslIdentityService.class, args);
	}

	@Bean
	public LocalValidatorFactoryBean validator(MessageSource messageSource) {
		LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
		bean.setValidationMessageSource(messageSource);
		return bean;
	}

	@Bean
	public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher eventPublisher) {
		return new DefaultAuthenticationEventPublisher(eventPublisher);
	}

	@Bean
	public NotificationClient notificationClient(@Value("${govNotify.key}") String key) {
		return new NotificationClient(key);
	}
}
