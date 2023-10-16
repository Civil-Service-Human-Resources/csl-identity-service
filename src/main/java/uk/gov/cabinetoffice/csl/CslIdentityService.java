package uk.gov.cabinetoffice.csl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import uk.gov.service.notify.NotificationClient;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
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

	@Bean(name = "loginAttemptCache")
	public Map<String, Integer> loginAttemptCache() {
		return new HashMap<>();
	}

	@Bean
	public NotificationClient notificationClient(@Value("${govNotify.key}") String key) {
		return new NotificationClient(key);
	}

//	TODO: To be implemented as part of the future ticket
//	@Bean
//	public RestTemplate restTemplate() {
//		return new RestTemplate();
//	}
}
